package com.marketplacehelper.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class HealthController {

    @GetMapping("/health")
    public Map<String, Object> health() {
        return Map.of(
            "status", "UP",
            "timestamp", LocalDateTime.now(),
            "service", "Marketplace Helper API",
            "version", "1.0.0"
        );
    }

    @GetMapping("/hello")
    public Map<String, String> hello() {
        return Map.of(
            "message", "Привет! Marketplace Helper API работает!",
            "description", "API для работы с Wildberries маркетплейсом"
        );
    }
}



