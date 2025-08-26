package com.bancolsr.api.repository;

import com.bancolsr.api.model.Cliente;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ClienteRepository extends JpaRepository<Cliente, String> {
    Cliente findByCpfAndAgenciaAndConta(String cpf, String agencia, String conta);
}
