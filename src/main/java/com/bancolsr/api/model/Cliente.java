package com.bancolsr.api.model;

import javax.persistence.Entity;
import javax.persistence.Id;

@Entity
public class Cliente {
    @Id
    private String cpf;
    private String agencia;
    private String conta;
    private Double limitePix;

    public Cliente() {}

    public Cliente(String cpf, String agencia, String conta, Double limitePix) {
        this.cpf = cpf;
        this.agencia = agencia;
        this.conta = conta;
        this.limitePix = limitePix;
    }

    public String getCpf() { return cpf; }
    public void setCpf(String cpf) { this.cpf = cpf; }
    public String getAgencia() { return agencia; }
    public void setAgencia(String agencia) { this.agencia = agencia; }
    public String getConta() { return conta; }
    public void setConta(String conta) { this.conta = conta; }
    public Double getLimitePix() { return limitePix; }
    public void setLimitePix(Double limitePix) { this.limitePix = limitePix; }
}
