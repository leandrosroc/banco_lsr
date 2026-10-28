package com.bancolsr.api.repository;

import com.bancolsr.api.model.Cliente;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import javax.persistence.LockModeType;
import java.util.Optional;

public interface ClienteRepository extends JpaRepository<Cliente, String> {

    Cliente findByCpfAndAgenciaAndConta(String cpf, String agencia, String conta);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT c FROM Cliente c WHERE c.cpf = :cpf AND c.agencia = :agencia AND c.conta = :conta")
    Optional<Cliente> findByCpfAndAgenciaAndContaWithLock(
            @Param("cpf") String cpf,
            @Param("agencia") String agencia,
            @Param("conta") String conta);
}
