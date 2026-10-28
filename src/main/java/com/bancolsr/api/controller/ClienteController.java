package com.bancolsr.api.controller;

import com.bancolsr.api.model.Cliente;
import com.bancolsr.api.repository.ClienteRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/clientes")
@Tag(name = "Clientes", description = "Gestão de cadastro e limites de clientes PIX")
public class ClienteController {

    @Autowired
    private ClienteRepository clienteRepository;

    @Operation(summary = "Cadastrar novo cliente com limite PIX")
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

    @Operation(summary = "Buscar dados do cliente por agência e conta")
    @GetMapping
    public ResponseEntity<Cliente> buscar(@RequestParam String cpf, @RequestParam String agencia, @RequestParam String conta) {
        Cliente cliente = clienteRepository.findByCpfAndAgenciaAndConta(cpf, agencia, conta);
        if (cliente == null) return ResponseEntity.notFound().build();
        return ResponseEntity.ok(cliente);
    }

    @Operation(summary = "Alterar limite PIX do cliente")
    @PutMapping("/limite")
    public ResponseEntity<Cliente> alterarLimite(@RequestParam String cpf, @RequestParam String agencia, @RequestParam String conta, @RequestParam Double novoLimite) {
        Cliente cliente = clienteRepository.findByCpfAndAgenciaAndConta(cpf, agencia, conta);
        if (cliente == null) return ResponseEntity.notFound().build();
        cliente.setLimitePix(novoLimite);
        return ResponseEntity.ok(clienteRepository.save(cliente));
    }

    @Operation(summary = "Remover cliente")
    @DeleteMapping
    public ResponseEntity<Void> remover(@RequestParam String cpf, @RequestParam String agencia, @RequestParam String conta) {
        Cliente cliente = clienteRepository.findByCpfAndAgenciaAndConta(cpf, agencia, conta);
        if (cliente == null) return ResponseEntity.notFound().build();
        clienteRepository.delete(cliente);
        return ResponseEntity.noContent().build();
    }
}
