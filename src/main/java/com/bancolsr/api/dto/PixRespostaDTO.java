package com.bancolsr.api.dto;

import com.bancolsr.api.model.StatusTransacaoPix;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

public class PixRespostaDTO {

    @Schema(description = "Indica se a transação foi aprovada e processada com sucesso", example = "true")
    private boolean sucesso;

    @Schema(description = "Status detalhado da transação", example = "SUCESSO")
    private StatusTransacaoPix status;

    @Schema(description = "Chave de idempotência da requisição", example = "e3f9479b-2c49-411f-8239-cf4d86c2f0a1")
    private String chaveIdempotencia;

    @Schema(description = "Identificador exclusivo ponta a ponta (E2E ID) do PIX", example = "E00000000202610271234567890abcdef")
    private String endToEndId;

    @Schema(description = "Mensagem explicativa sobre o resultado do processamento", example = "Transação realizada com sucesso.")
    private String mensagem;

    @Schema(description = "Valor transferido", example = "150.00")
    private Double valor;

    @Schema(description = "Limite PIX restante da conta após a transação", example = "850.00")
    private Double limiteRestante;

    @Schema(description = "Data e hora do processamento")
    private LocalDateTime dataHora;

    @Schema(description = "Indica se a resposta veio de um cache de idempotência (evitou pagar novamente por requisição repetida)", example = "false")
    private boolean reprocessada;

    @Schema(description = "Indica se a transação caiu no alerta de suspeita de duplicidade temporal", example = "false")
    private boolean alertaDuplicidade;

    public PixRespostaDTO() {}

    public static PixRespostaDTO sucesso(String chaveIdempotencia, String endToEndId, Double valor, Double limiteRestante, LocalDateTime dataHora, boolean reprocessada, String mensagem) {
        PixRespostaDTO dto = new PixRespostaDTO();
        dto.sucesso = true;
        dto.status = StatusTransacaoPix.SUCESSO;
        dto.chaveIdempotencia = chaveIdempotencia;
        dto.endToEndId = endToEndId;
        dto.valor = valor;
        dto.limiteRestante = limiteRestante;
        dto.dataHora = dataHora;
        dto.reprocessada = reprocessada;
        dto.alertaDuplicidade = false;
        dto.mensagem = mensagem;
        return dto;
    }

    public static PixRespostaDTO erro(StatusTransacaoPix status, String chaveIdempotencia, String endToEndId, Double valor, Double limiteAtual, String mensagem, boolean alertaDuplicidade) {
        PixRespostaDTO dto = new PixRespostaDTO();
        dto.sucesso = false;
        dto.status = status;
        dto.chaveIdempotencia = chaveIdempotencia;
        dto.endToEndId = endToEndId;
        dto.valor = valor;
        dto.limiteRestante = limiteAtual;
        dto.dataHora = LocalDateTime.now();
        dto.reprocessada = false;
        dto.alertaDuplicidade = alertaDuplicidade;
        dto.mensagem = mensagem;
        return dto;
    }

    public boolean isSucesso() { return sucesso; }
    public void setSucesso(boolean sucesso) { this.sucesso = sucesso; }

    public StatusTransacaoPix getStatus() { return status; }
    public void setStatus(StatusTransacaoPix status) { this.status = status; }

    public String getChaveIdempotencia() { return chaveIdempotencia; }
    public void setChaveIdempotencia(String chaveIdempotencia) { this.chaveIdempotencia = chaveIdempotencia; }

    public String getEndToEndId() { return endToEndId; }
    public void setEndToEndId(String endToEndId) { this.endToEndId = endToEndId; }

    public String getMensagem() { return mensagem; }
    public void setMensagem(String mensagem) { this.mensagem = mensagem; }

    public Double getValor() { return valor; }
    public void setValor(Double valor) { this.valor = valor; }

    public Double getLimiteRestante() { return limiteRestante; }
    public void setLimiteRestante(Double limiteRestante) { this.limiteRestante = limiteRestante; }

    public LocalDateTime getDataHora() { return dataHora; }
    public void setDataHora(LocalDateTime dataHora) { this.dataHora = dataHora; }

    public boolean isReprocessada() { return reprocessada; }
    public void setReprocessada(boolean reprocessada) { this.reprocessada = reprocessada; }

    public boolean isAlertaDuplicidade() { return alertaDuplicidade; }
    public void setAlertaDuplicidade(boolean alertaDuplicidade) { this.alertaDuplicidade = alertaDuplicidade; }
}

