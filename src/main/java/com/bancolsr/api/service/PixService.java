package com.bancolsr.api.service;

import com.bancolsr.api.dto.PixRequisicaoDTO;
import com.bancolsr.api.dto.PixRespostaDTO;
import com.bancolsr.api.model.Cliente;
import com.bancolsr.api.model.StatusTransacaoPix;
import com.bancolsr.api.model.TransacaoPix;
import com.bancolsr.api.repository.ClienteRepository;
import com.bancolsr.api.repository.TransacaoPixRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class PixService {

    private static final int JANELA_DUPLICIDADE_SEGUNDOS = 60;

    @Autowired
    private ClienteRepository clienteRepository;

    @Autowired
    private TransacaoPixRepository transacaoPixRepository;

    /**
     * Avaliação simples de viabilidade sem consumir limite (somente leitura).
     * Corrige o comportamento anterior onde requisições GET debitavam saldo.
     */
    public boolean podeRealizarPix(String cpf, String agencia, String conta, Double valorPix) {
        if (valorPix == null || valorPix <= 0) return false;
        Cliente cliente = clienteRepository.findByCpfAndAgenciaAndConta(cpf, agencia, conta);
        if (cliente == null) return false;
        return cliente.getLimitePix() != null && cliente.getLimitePix() >= valorPix;
    }

    /**
     * Executa a transação PIX com garantias estritas de:
     * 1. Idempotência (evita reprocessamento por retries ou duplo clique)
     * 2. Trava temporal contra transações duplicadas acidentais/fraude
     * 3. Validação de integridade de valores
     * 4. Bloqueio pessimista de concorrência (evita race conditions)
     * 5. Registro de auditoria E2E
     */
    @Transactional
    public PixRespostaDTO transferirPix(PixRequisicaoDTO requisicao) {
        // Normaliza ou gera chave de idempotência
        String chaveIdempotencia = requisicao.getChaveIdempotencia();
        if (chaveIdempotencia == null || chaveIdempotencia.trim().isEmpty()) {
            chaveIdempotencia = UUID.randomUUID().toString();
            requisicao.setChaveIdempotencia(chaveIdempotencia);
        } else {
            chaveIdempotencia = chaveIdempotencia.trim();
        }

        // TRAVA 1: IDEMPOTÊNCIA ESTRITA
        // Se a requisição já foi processada anteriormente com essa chave, devolve o resultado idêntico sem novo débito
        Optional<TransacaoPix> transacaoExistente = transacaoPixRepository.findByChaveIdempotencia(chaveIdempotencia);
        if (transacaoExistente.isPresent()) {
            TransacaoPix existente = transacaoExistente.get();
            Cliente cliente = clienteRepository.findByCpfAndAgenciaAndConta(
                    existente.getCpfOrigem(), existente.getAgenciaOrigem(), existente.getContaOrigem());
            Double limiteAtual = cliente != null ? cliente.getLimitePix() : null;

            if (existente.getStatus() == StatusTransacaoPix.SUCESSO) {
                return PixRespostaDTO.sucesso(
                        existente.getChaveIdempotencia(),
                        existente.getEndToEndId(),
                        existente.getValor(),
                        limiteAtual,
                        existente.getDataHora(),
                        true,
                        "Transação já processada com sucesso anteriormente. Débito NÃO foi duplicado (Idempotência garantida)."
                );
            } else {
                return PixRespostaDTO.erro(
                        existente.getStatus(),
                        existente.getChaveIdempotencia(),
                        existente.getEndToEndId(),
                        existente.getValor(),
                        limiteAtual,
                        "Transação repetida já finalizada anteriormente com status: " + existente.getStatus() + " (" + existente.getMotivo() + ")",
                        false
                );
            }
        }

        // TRAVA 2: VALIDAÇÃO DE VALOR E DESTINO
        if (requisicao.getValor() == null || requisicao.getValor() <= 0) {
            String endToEndId = gerarEndToEndId();
            registrarTransacao(chaveIdempotencia, endToEndId, requisicao, StatusTransacaoPix.NEGADA_VALOR_INVALIDO, "Valor do PIX deve ser maior que zero");
            return PixRespostaDTO.erro(
                    StatusTransacaoPix.NEGADA_VALOR_INVALIDO,
                    chaveIdempotencia,
                    endToEndId,
                    requisicao.getValor(),
                    null,
                    "Transação negada: o valor do PIX deve ser positivo e maior que zero.",
                    false
            );
        }

        if (requisicao.getChavePixDestino() == null || requisicao.getChavePixDestino().trim().isEmpty()) {
            String endToEndId = gerarEndToEndId();
            registrarTransacao(chaveIdempotencia, endToEndId, requisicao, StatusTransacaoPix.NEGADA_VALOR_INVALIDO, "Chave PIX destino obrigatória");
            return PixRespostaDTO.erro(
                    StatusTransacaoPix.NEGADA_VALOR_INVALIDO,
                    chaveIdempotencia,
                    endToEndId,
                    requisicao.getValor(),
                    null,
                    "Transação negada: chave PIX de destino não informada.",
                    false
            );
        }

        // TRAVA 3: PROTEÇÃO CONTRA DUPLICIDADE TEMPORAL (ANTIFRAUDE / DUPLO PAGAMENTO ACIDENTAL)
        // Se houver pagamento com mesmo pagador, recebedor e valor nos últimos 60 segundos, exige confirmação explícita
        if (!requisicao.isConfirmarSuspeitaDuplicada()) {
            LocalDateTime dataLimite = LocalDateTime.now().minusSeconds(JANELA_DUPLICIDADE_SEGUNDOS);
            List<TransacaoPix> duplicadas = transacaoPixRepository.findTransacoesSuspeitasDuplicadas(
                    requisicao.getCpf(),
                    requisicao.getChavePixDestino(),
                    requisicao.getValor(),
                    dataLimite
            );

            if (!duplicadas.isEmpty()) {
                String endToEndId = gerarEndToEndId();
                registrarTransacao(chaveIdempotencia, endToEndId, requisicao, StatusTransacaoPix.NEGADA_SUSPEITA_DUPLICIDADE, "Suspeita de transação duplicada em menos de " + JANELA_DUPLICIDADE_SEGUNDOS + " segundos");
                return PixRespostaDTO.erro(
                        StatusTransacaoPix.NEGADA_SUSPEITA_DUPLICIDADE,
                        chaveIdempotencia,
                        endToEndId,
                        requisicao.getValor(),
                        null,
                        "ALERTA DE SUSPEITA DE DUPLICIDADE: Uma transação idêntica (mesmo valor e destino) foi concluída nos últimos " +
                                JANELA_DUPLICIDADE_SEGUNDOS + " segundos. Para confirmar um segundo pagamento idêntico intencional, envie 'confirmarSuspeitaDuplicada: true'.",
                        true
                );
            }
        }

        // TRAVA 4: BLOQUEIO PESSIMISTA DE CONCORRÊNCIA (RACE CONDITIONS / DOUBLE-SPENDING)
        Optional<Cliente> clienteOpt = clienteRepository.findByCpfAndAgenciaAndContaWithLock(
                requisicao.getCpf(), requisicao.getAgencia(), requisicao.getConta());

        String endToEndId = gerarEndToEndId();

        if (clienteOpt.isEmpty()) {
            registrarTransacao(chaveIdempotencia, endToEndId, requisicao, StatusTransacaoPix.NEGADA_CONTA_INEXISTENTE, "Conta inexistente");
            return PixRespostaDTO.erro(
                    StatusTransacaoPix.NEGADA_CONTA_INEXISTENTE,
                    chaveIdempotencia,
                    endToEndId,
                    requisicao.getValor(),
                    null,
                    "Transação negada: conta ou cliente de origem inexistente.",
                    false
            );
        }

        Cliente cliente = clienteOpt.get();
        if (cliente.getLimitePix() == null || cliente.getLimitePix() < requisicao.getValor()) {
            registrarTransacao(chaveIdempotencia, endToEndId, requisicao, StatusTransacaoPix.NEGADA_LIMITE, "Limite insuficiente");
            return PixRespostaDTO.erro(
                    StatusTransacaoPix.NEGADA_LIMITE,
                    chaveIdempotencia,
                    endToEndId,
                    requisicao.getValor(),
                    cliente.getLimitePix(),
                    "Transação negada: limite insuficiente.",
                    false
            );
        }

        // EFETIVA O DÉBITO
        double novoLimite = cliente.getLimitePix() - requisicao.getValor();
        cliente.setLimitePix(novoLimite);
        clienteRepository.save(cliente);

        TransacaoPix transacaoSucesso = registrarTransacao(
                chaveIdempotencia, endToEndId, requisicao, StatusTransacaoPix.SUCESSO, "Transação concluída com sucesso");

        return PixRespostaDTO.sucesso(
                transacaoSucesso.getChaveIdempotencia(),
                transacaoSucesso.getEndToEndId(),
                transacaoSucesso.getValor(),
                novoLimite,
                transacaoSucesso.getDataHora(),
                false,
                "Transação PIX realizada com sucesso."
        );
    }

    public List<TransacaoPix> listarTransacoesPorCpf(String cpf) {
        return transacaoPixRepository.findByCpfOrigemOrderByDataHoraDesc(cpf);
    }

    public Optional<TransacaoPix> buscarTransacaoPorIdempotencia(String chaveIdempotencia) {
        return transacaoPixRepository.findByChaveIdempotencia(chaveIdempotencia);
    }

    public Optional<TransacaoPix> buscarTransacaoPorEndToEndId(String endToEndId) {
        return transacaoPixRepository.findByEndToEndId(endToEndId);
    }

    private TransacaoPix registrarTransacao(String chaveIdempotencia, String endToEndId, PixRequisicaoDTO req, StatusTransacaoPix status, String motivo) {
        TransacaoPix transacao = new TransacaoPix(
                chaveIdempotencia,
                endToEndId,
                req.getCpf(),
                req.getAgencia(),
                req.getConta(),
                req.getChavePixDestino(),
                req.getValor(),
                LocalDateTime.now(),
                status,
                motivo
        );
        return transacaoPixRepository.save(transacao);
    }

    private String gerarEndToEndId() {
        String data = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        String rand = UUID.randomUUID().toString().replace("-", "").substring(0, 10).toUpperCase();
        return "E00000000" + data + rand;
    }
}
