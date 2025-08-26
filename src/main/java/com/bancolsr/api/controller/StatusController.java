package com.bancolsr.api.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import java.util.HashMap;
import java.util.Map;

@RestController
public class StatusController {
    @Autowired(required = false)
    private JdbcTemplate jdbcTemplate;

    @GetMapping("/healthcheck")
    public ResponseEntity<Map<String, Object>> healthcheck() {
        Map<String, Object> status = new HashMap<>();
        status.put("api", "OK");
        try {
            jdbcTemplate.queryForObject("SELECT 1", Integer.class);
            status.put("database", "OK");
        } catch (Exception e) {
            status.put("database", "ERROR");
        }
        return ResponseEntity.ok(status);
    }

    @GetMapping("/")
    public ResponseEntity<Map<String, Object>> root() {
        Map<String, Object> info = new HashMap<>();
        info.put("title", "Banco LSR API PIX");
        info.put("version", "1.0.0");
        info.put("swagger", "/swagger-ui.html");
        return ResponseEntity.ok(info);
    }
}
