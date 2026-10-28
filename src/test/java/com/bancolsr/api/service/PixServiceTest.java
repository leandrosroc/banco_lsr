package com.bancolsr.api.service;

import com.bancolsr.api.dto.PixRequisicaoDTO;
import com.bancolsr.api.dto.PixRespostaDTO;
import com.bancolsr.api.model.Cliente;
import com.bancolsr.api.model.StatusTransacaoPix;
import com.bancolsr.api.model.TransacaoPix;
import com.bancolsr.api.repository.ClienteRepository;
import com.bancolsr.api.repository.TransacaoPixRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PixServiceTest {

    @Mock
    private ClienteRepository clienteRepository;

    @Mock
    private TransacaoPixRepository transacaoPixRepository;

    @InjectMocks
    private PixService pixService;

    private Cliente clientePadrao;

    @BeforeEach
    void setUp() {
        clientePadrao = new Cliente("12345678900", "0001", "12345-6", 1000.0);
    }

    @Test
    @DisplayName("Garante que chamada repetida com mesma chave de idempotência NÃO debita duas vezes")
    void deveGarantirIdempotenciaSemDebitarDuasVezes() {
        String chaveIdempotencia = "key-uuid-123";
        TransacaoPix transacaoAnterior = new TransacaoPix(
                chaveIdempotencia, "E000001", "12345678900", "0001", "12345-6",
                "destino@pix.com", 200.0, LocalDateTime.now(), StatusTransacaoPix.SUCESSO, "Transação concluída"
        );

        when(transacaoPixRepository.findByChaveIdempotencia(chaveIdempotencia))
                .thenReturn(Optional.of(transacaoAnterior));
        when(clienteRepository.findByCpfAndAgenciaAndConta("12345678900", "0001", "12345-6"))
                .thenReturn(clientePadrao);

        PixRequisicaoDTO req = new PixRequisicaoDTO(
                "12345678900", "0001", "12345-6", "destino@pix.com", 200.0, chaveIdempotencia, false
        );

        PixRespostaDTO resposta = pixService.transferirPix(req);

        assertTrue(resposta.isSucesso());
        assertTrue(resposta.isReprocessada());
        assertEquals("E000001", resposta.getEndToEndId());
        // Não deve interagir com lock nem salvar cliente novamente
        verify(clienteRepository, never()).findByCpfAndAgenciaAndContaWithLock(any(), any(), any());
        verify(clienteRepository, never()).save(any());
    }

    @Test
    @DisplayName("Deve bloquear transação por suspeita de duplicidade em menos de 60 segundos")
    void deveBloquearSuspeitaDeDuplicidadeTemporal() {
        PixRequisicaoDTO req = new PixRequisicaoDTO(
                "12345678900", "0001", "12345-6", "destino@pix.com", 200.0, "nova-chave-456", false
        );

        when(transacaoPixRepository.findByChaveIdempotencia("nova-chave-456")).thenReturn(Optional.empty());

        TransacaoPix transacaoRecente = new TransacaoPix(
                "chave-anterior", "E000002", "12345678900", "0001", "12345-6",
                "destino@pix.com", 200.0, LocalDateTime.now().minusSeconds(10), StatusTransacaoPix.SUCESSO, "Sucesso"
        );
        when(transacaoPixRepository.findTransacoesSuspeitasDuplicadas(eq("12345678900"), eq("destino@pix.com"), eq(200.0), any()))
                .thenReturn(List.of(transacaoRecente));

        PixRespostaDTO resposta = pixService.transferirPix(req);

        assertFalse(resposta.isSucesso());
        assertEquals(StatusTransacaoPix.NEGADA_SUSPEITA_DUPLICIDADE, resposta.getStatus());
        assertTrue(resposta.isAlertaDuplicidade());
        verify(clienteRepository, never()).save(any());
    }

    @Test
    @DisplayName("Deve permitir segunda transação idêntica se confirmação for explícita")
    void devePermitirDuplicidadeComConfirmacaoExplicita() {
        PixRequisicaoDTO req = new PixRequisicaoDTO(
                "12345678900", "0001", "12345-6", "destino@pix.com", 200.0, "nova-chave-789", true
        );

        when(transacaoPixRepository.findByChaveIdempotencia("nova-chave-789")).thenReturn(Optional.empty());
        when(clienteRepository.findByCpfAndAgenciaAndContaWithLock("12345678900", "0001", "12345-6"))
                .thenReturn(Optional.of(clientePadrao));
        when(transacaoPixRepository.save(any(TransacaoPix.class))).thenAnswer(i -> i.getArgument(0));

        PixRespostaDTO resposta = pixService.transferirPix(req);

        assertTrue(resposta.isSucesso());
        assertEquals(800.0, clientePadrao.getLimitePix());
        verify(clienteRepository).save(clientePadrao);
    }

    @Test
    @DisplayName("Deve rejeitar valor nulo ou negativo")
    void deveRejeitarValorInvalido() {
        PixRequisicaoDTO req = new PixRequisicaoDTO(
                "12345678900", "0001", "12345-6", "destino@pix.com", -50.0, null, false
        );

        PixRespostaDTO resposta = pixService.transferirPix(req);

        assertFalse(resposta.isSucesso());
        assertEquals(StatusTransacaoPix.NEGADA_VALOR_INVALIDO, resposta.getStatus());
        verify(clienteRepository, never()).findByCpfAndAgenciaAndContaWithLock(any(), any(), any());
    }

    @Test
    @DisplayName("Deve rejeitar transferência quando limite for insuficiente")
    void deveRejeitarQuandoLimiteInsuficiente() {
        PixRequisicaoDTO req = new PixRequisicaoDTO(
                "12345678900", "0001", "12345-6", "destino@pix.com", 1500.0, "chave-limite", false
        );

        when(transacaoPixRepository.findByChaveIdempotencia("chave-limite")).thenReturn(Optional.empty());
        when(transacaoPixRepository.findTransacoesSuspeitasDuplicadas(any(), any(), any(), any()))
                .thenReturn(Collections.emptyList());
        when(clienteRepository.findByCpfAndAgenciaAndContaWithLock("12345678900", "0001", "12345-6"))
                .thenReturn(Optional.of(clientePadrao));

        PixRespostaDTO resposta = pixService.transferirPix(req);

        assertFalse(resposta.isSucesso());
        assertEquals(StatusTransacaoPix.NEGADA_LIMITE, resposta.getStatus());
        assertEquals(1000.0, clientePadrao.getLimitePix()); // Limite inalterado
    }
}

