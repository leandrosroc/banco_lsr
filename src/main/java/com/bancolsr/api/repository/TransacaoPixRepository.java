package com.bancolsr.api.repository;

import com.bancolsr.api.model.TransacaoPix;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface TransacaoPixRepository extends JpaRepository<TransacaoPix, Long> {

    Optional<TransacaoPix> findByChaveIdempotencia(String chaveIdempotencia);

    Optional<TransacaoPix> findByEndToEndId(String endToEndId);

    List<TransacaoPix> findByCpfOrigemOrderByDataHoraDesc(String cpfOrigem);

    @Query("SELECT t FROM TransacaoPix t WHERE t.cpfOrigem = :cpf " +
           "AND t.chavePixDestino = :chaveDestino " +
           "AND t.valor = :valor " +
           "AND t.status = com.bancolsr.api.model.StatusTransacaoPix.SUCESSO " +
           "AND t.dataHora >= :dataLimite")
    List<TransacaoPix> findTransacoesSuspeitasDuplicadas(
            @Param("cpf") String cpf,
            @Param("chaveDestino") String chaveDestino,
            @Param("valor") Double valor,
            @Param("dataLimite") LocalDateTime dataLimite);
}

