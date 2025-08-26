package com.bancolsr.api.controller;

import com.bancolsr.api.model.Cliente;
import com.bancolsr.api.repository.ClienteRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.Optional;

@RestController
@RequestMapping("/clientes")
public class ClienteController {
    @Autowired
    private ClienteRepository clienteRepository;

    // 2.1 Cadastrar cliente
    @PostMapping
    public ResponseEntity<Cliente> cadastrar(@RequestBody Cliente cliente) {
        if (cliente.getCpf() == null || cliente.getAgencia() == null || cliente.getConta() == null || cliente.getLimitePix() == null) {
            return ResponseEntity.badRequest().build();
        }
        if (clienteRepository.existsById(cliente.getCpf())) {
            return ResponseEntity.status(409).build(); // conflito
        }
        return ResponseEntity.ok(clienteRepository.save(cliente));
    }

    // 2.2 Buscar cliente
    @GetMapping
    public ResponseEntity<Cliente> buscar(@RequestParam String cpf, @RequestParam String agencia, @RequestParam String conta) {
        Cliente cliente = clienteRepository.findByCpfAndAgenciaAndConta(cpf, agencia, conta);
        if (cliente == null) return ResponseEntity.notFound().build();
        return ResponseEntity.ok(cliente);
    }

    // 2.3 Alterar limite
    @PutMapping("/limite")
    public ResponseEntity<Cliente> alterarLimite(@RequestParam String cpf, @RequestParam String agencia, @RequestParam String conta, @RequestParam Double novoLimite) {
        Cliente cliente = clienteRepository.findByCpfAndAgenciaAndConta(cpf, agencia, conta);
        if (cliente == null) return ResponseEntity.notFound().build();
        cliente.setLimitePix(novoLimite);
        return ResponseEntity.ok(clienteRepository.save(cliente));
    }

    // 2.4 Remover cliente
    @DeleteMapping
    public ResponseEntity<Void> remover(@RequestParam String cpf, @RequestParam String agencia, @RequestParam String conta) {
        Cliente cliente = clienteRepository.findByCpfAndAgenciaAndConta(cpf, agencia, conta);
        if (cliente == null) return ResponseEntity.notFound().build();
        clienteRepository.delete(cliente);
        return ResponseEntity.noContent().build();
    }
}
