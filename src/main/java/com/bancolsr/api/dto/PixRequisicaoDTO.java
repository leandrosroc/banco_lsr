package com.bancolsr.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;

public class PixRequisicaoDTO {

    @Schema(description = "CPF do pagador", example = "12345678900", required = true)
    private String cpf;

    @Schema(description = "Agência bancária", example = "0001", required = true)
    private String agencia;

    @Schema(description = "Conta bancária", example = "12345-6", required = true)
    private String conta;

    @Schema(description = "Chave PIX do recebedor (CPF, e-mail, telefone ou chave aleatória)", example = "destinatario@email.com", required = true)
    private String chavePixDestino;

    @Schema(description = "Valor do PIX a ser transferido", example = "150.00", required = true)
    private Double valor;

    @Schema(description = "Chave de Idempotência única (UUID) para evitar pagamentos duplicados por retries/duplo clique", example = "e3f9479b-2c49-411f-8239-cf4d86c2f0a1")
    private String chaveIdempotencia;

    @Schema(description = "Se true, autoriza a transação mesmo se houver suspeita de duplicidade recente para o mesmo recebedor e valor", example = "false")
    private boolean confirmarSuspeitaDuplicada;

    public PixRequisicaoDTO() {}

    public PixRequisicaoDTO(String cpf, String agencia, String conta, String chavePixDestino, Double valor, String chaveIdempotencia, boolean confirmarSuspeitaDuplicada) {
        this.cpf = cpf;
        this.agencia = agencia;
        this.conta = conta;
        this.chavePixDestino = chavePixDestino;
        this.valor = valor;
        this.chaveIdempotencia = chaveIdempotencia;
        this.confirmarSuspeitaDuplicada = confirmarSuspeitaDuplicada;
    }

    public String getCpf() { return cpf; }
    public void setCpf(String cpf) { this.cpf = cpf; }

    public String getAgencia() { return agencia; }
    public void setAgencia(String agencia) { this.agencia = agencia; }

    public String getConta() { return conta; }
    public void setConta(String conta) { this.conta = conta; }

    public String getChavePixDestino() { return chavePixDestino; }
    public void setChavePixDestino(String chavePixDestino) { this.chavePixDestino = chavePixDestino; }

    public Double getValor() { return valor; }
    public void setValor(Double valor) { this.valor = valor; }

    public String getChaveIdempotencia() { return chaveIdempotencia; }
    public void setChaveIdempotencia(String chaveIdempotencia) { this.chaveIdempotencia = chaveIdempotencia; }

    public boolean isConfirmarSuspeitaDuplicada() { return confirmarSuspeitaDuplicada; }
    public void setConfirmarSuspeitaDuplicada(boolean confirmarSuspeitaDuplicada) { this.confirmarSuspeitaDuplicada = confirmarSuspeitaDuplicada; }
}

