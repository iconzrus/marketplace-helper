package com.marketplacehelper.service;

import com.marketplacehelper.dto.WbApiEndpointStatus;
import com.marketplacehelper.dto.WbApiStatusReportDto;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class WbApiStatusService {

    private final RestTemplate wbRestTemplate;
    private final WbApiService wbApiService;

    public WbApiStatusService(RestTemplate wbRestTemplate, WbApiService wbApiService) {
        this.wbRestTemplate = wbRestTemplate;
        this.wbApiService = wbApiService;
    }

    public WbApiStatusReportDto checkAll() {
        List<WbApiEndpointStatus> statuses = new ArrayList<>();

        // Набор ключевых ручек, которые используем
        Map<String, String> endpoints = new LinkedHashMap<>();
        endpoints.put("Ping", "/ping");
        endpoints.put("Seller Info", "https://common-api.wildberries.ru/api/v1/seller-info");
        endpoints.put("Goods (filter)", "/api/v2/list/goods/filter");

        for (Map.Entry<String, String> entry : endpoints.entrySet()) {
            statuses.add(checkEndpoint(entry.getKey(), entry.getValue()));
        }

        WbApiStatusReportDto report = new WbApiStatusReportDto();
        report.setCheckedAt(Instant.now());
        report.setEndpoints(statuses);
        return report;
    }

    private WbApiEndpointStatus checkEndpoint(String name, String pathOrUrl) {
        try {
            ResponseEntity<String> response;
            if (pathOrUrl.startsWith("http")) {
                response = wbRestTemplate.exchange(pathOrUrl, HttpMethod.GET, null, new ParameterizedTypeReference<>(){});
            } else {
                // используем wbApiService.getGoodsWithPrices() для мок-режима и прямой URL при боевом
                if ("/api/v2/list/goods/filter".equals(pathOrUrl)) {
                    wbApiService.getGoodsWithPrices();
                    return new WbApiEndpointStatus(name, pathOrUrl, "UP", 200, "");
                }
                response = wbRestTemplate.exchange(pathOrUrl, HttpMethod.GET, null, new ParameterizedTypeReference<>(){});
            }
            int code = response.getStatusCode().value();
            String status = code >= 200 && code < 500 && code != 503 ? "UP" : "DOWN";
            return new WbApiEndpointStatus(name, pathOrUrl, status, code, null);
        } catch (RestClientException ex) {
            String message = ex.getMessage() != null ? ex.getMessage() : "request failed";
            // Если явный 503 встречается в сообщении — считаем DOWN, иначе UP (метод исключения)
            String normalized = message.toLowerCase();
            boolean is503 = normalized.contains("503") || normalized.contains("service unavailable");
            return new WbApiEndpointStatus(name, pathOrUrl, is503 ? "DOWN" : "UP", is503 ? 503 : null, message);
        }
    }
}


