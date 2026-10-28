package com.bancolsr.api.controller;

import com.bancolsr.api.dto.PixRequisicaoDTO;
import com.bancolsr.api.dto.PixRespostaDTO;
import com.bancolsr.api.model.TransacaoPix;
import com.bancolsr.api.service.PixService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/pix")
@Tag(name = "PIX", description = "Operações de pagamento, validação de limites e travas antifraude/duplicidade")
public class PixController {

    @Autowired
    private PixService pixService;

    @Operation(summary = "Realizar pagamento PIX com travas de idempotência e antifraude",
               description = "Processa a transferência PIX. Caso receba a mesma 'chaveIdempotencia', " +
                             "garante que o valor NÃO será debitado duas vezes. Possui trava contra transferências idênticas em menos de 60 segundos.")
    @PostMapping("/pagar")
    public ResponseEntity<PixRespostaDTO> pagarPix(@RequestBody PixRequisicaoDTO requisicao) {
        PixRespostaDTO resposta = pixService.transferirPix(requisicao);
        if (resposta.isSucesso()) {
            return ResponseEntity.ok(resposta);
        }

        switch (resposta.getStatus()) {
            case NEGADA_SUSPEITA_DUPLICIDADE:
                return ResponseEntity.status(HttpStatus.CONFLICT).body(resposta);
            case NEGADA_CONTA_INEXISTENTE:
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(resposta);
            case NEGADA_VALOR_INVALIDO:
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(resposta);
            case NEGADA_LIMITE:
            default:
                return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).body(resposta);
        }
    }

    @Operation(summary = "Avaliar limite PIX (Consulta Segura - Somente Leitura)",
               description = "Apenas consulta se a conta tem limite suficiente para realizar a transação, sem alterar o saldo ou debitar o limite.")
    @GetMapping("/avaliar")
    public ResponseEntity<String> avaliarPix(@RequestParam String cpf,
                                            @RequestParam String agencia,
                                            @RequestParam String conta,
                                            @RequestParam Double valorPix) {
        boolean podeRealizar = pixService.podeRealizarPix(cpf, agencia, conta, valorPix);
        if (podeRealizar) {
            return ResponseEntity.ok("Transação permitida: conta possui limite suficiente.");
        } else {
            return ResponseEntity.ok("Transação negada: limite insuficiente, valor inválido ou conta inexistente.");
        }
    }

    @Operation(summary = "Consultar histórico de transações de um cliente")
    @GetMapping("/transacoes")
    public ResponseEntity<List<TransacaoPix>> listarTransacoes(@RequestParam String cpf) {
        return ResponseEntity.ok(pixService.listarTransacoesPorCpf(cpf));
    }

    @Operation(summary = "Consultar comprovante de transação por Chave de Idempotência")
    @GetMapping("/transacoes/idempotencia/{chave}")
    public ResponseEntity<TransacaoPix> buscarPorIdempotencia(@PathVariable String chave) {
        return pixService.buscarTransacaoPorIdempotencia(chave)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
}
