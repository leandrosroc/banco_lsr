package com.bancolsr.api.service;

import com.bancolsr.api.model.Cliente;
import com.bancolsr.api.repository.ClienteRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class PixService {
    @Autowired
    private ClienteRepository clienteRepository;

    public boolean podeRealizarPix(String cpf, String agencia, String conta, Double valorPix) {
        Cliente cliente = clienteRepository.findByCpfAndAgenciaAndConta(cpf, agencia, conta);
        if (cliente == null) return false;
        if (cliente.getLimitePix() < valorPix) return false;
        // Consome o limite
        cliente.setLimitePix(cliente.getLimitePix() - valorPix);
        clienteRepository.save(cliente);
        return true;
    }
}
