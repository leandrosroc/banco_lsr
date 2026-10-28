package com.bancolsr.api.model;

import javax.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "transacao_pix", indexes = {
    @Index(name = "idx_chave_idempotencia", columnList = "chaveIdempotencia", unique = true),
    @Index(name = "idx_end_to_end", columnList = "endToEndId", unique = true),
    @Index(name = "idx_cpf_origem", columnList = "cpfOrigem")
})
public class TransacaoPix {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 100)
    private String chaveIdempotencia;

    @Column(nullable = false, unique = true, length = 100)
    private String endToEndId;

    @Column(nullable = false, length = 14)
    private String cpfOrigem;

    @Column(nullable = false, length = 10)
    private String agenciaOrigem;

    @Column(nullable = false, length = 20)
    private String contaOrigem;

    @Column(nullable = false, length = 100)
    private String chavePixDestino;

    @Column(nullable = false)
    private Double valor;

    @Column(nullable = false)
    private LocalDateTime dataHora;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private StatusTransacaoPix status;

    @Column(length = 255)
    private String motivo;

    public TransacaoPix() {}

    public TransacaoPix(String chaveIdempotencia, String endToEndId, String cpfOrigem,
                        String agenciaOrigem, String contaOrigem, String chavePixDestino,
                        Double valor, LocalDateTime dataHora, StatusTransacaoPix status, String motivo) {
        this.chaveIdempotencia = chaveIdempotencia;
        this.endToEndId = endToEndId;
        this.cpfOrigem = cpfOrigem;
        this.agenciaOrigem = agenciaOrigem;
        this.contaOrigem = contaOrigem;
        this.chavePixDestino = chavePixDestino;
        this.valor = valor;
        this.dataHora = dataHora;
        this.status = status;
        this.motivo = motivo;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getChaveIdempotencia() { return chaveIdempotencia; }
    public void setChaveIdempotencia(String chaveIdempotencia) { this.chaveIdempotencia = chaveIdempotencia; }

    public String getEndToEndId() { return endToEndId; }
    public void setEndToEndId(String endToEndId) { this.endToEndId = endToEndId; }

    public String getCpfOrigem() { return cpfOrigem; }
    public void setCpfOrigem(String cpfOrigem) { this.cpfOrigem = cpfOrigem; }

    public String getAgenciaOrigem() { return agenciaOrigem; }
    public void setAgenciaOrigem(String agenciaOrigem) { this.agenciaOrigem = agenciaOrigem; }

    public String getContaOrigem() { return contaOrigem; }
    public void setContaOrigem(String contaOrigem) { this.contaOrigem = contaOrigem; }

    public String getChavePixDestino() { return chavePixDestino; }
    public void setChavePixDestino(String chavePixDestino) { this.chavePixDestino = chavePixDestino; }

    public Double getValor() { return valor; }
    public void setValor(Double valor) { this.valor = valor; }

    public LocalDateTime getDataHora() { return dataHora; }
    public void setDataHora(LocalDateTime dataHora) { this.dataHora = dataHora; }

    public StatusTransacaoPix getStatus() { return status; }
    public void setStatus(StatusTransacaoPix status) { this.status = status; }

    public String getMotivo() { return motivo; }
    public void setMotivo(String motivo) { this.motivo = motivo; }
}

