package com.bancolsr.api.controller;

import com.bancolsr.api.service.PixService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/pix")
public class PixController {
    @Autowired
    private PixService pixService;

    @GetMapping("/avaliar")
    public String avaliarPix(@RequestParam String cpf,
                             @RequestParam String agencia,
                             @RequestParam String conta,
                             @RequestParam Double valorPix) {
        boolean podeRealizar = pixService.podeRealizarPix(cpf, agencia, conta, valorPix);
        return podeRealizar ? "Transação permitida" : "Transação negada: limite insuficiente ou conta inexistente";
    }
}
