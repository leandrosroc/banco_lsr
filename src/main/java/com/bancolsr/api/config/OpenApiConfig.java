package com.bancolsr.api.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {
    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Banco LSR - API PIX")
                        .version("2.0.0")
                        .description("API segura para transferências PIX com proteção antifraude, chave de idempotência contra pagamentos duplicados, bloqueio pessimista de concorrência e auditoria de transações.")
                        .contact(new Contact().name("Equipe de Arquitetura Banco LSR")));
    }
}
