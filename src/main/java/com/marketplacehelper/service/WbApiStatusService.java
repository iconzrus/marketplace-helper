package com.marketplacehelper.service;

import com.marketplacehelper.dto.WbApiEndpointStatus;
import com.marketplacehelper.dto.WbApiStatusReportDto;
import org.springframework.beans.factory.annotation.Value;
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
    private final String wbBaseUrl;

    public WbApiStatusService(RestTemplate wbRestTemplate,
                              @Value("${wb.api.base-url:https://marketplace-api.wildberries.ru}") String wbBaseUrl) {
        this.wbRestTemplate = wbRestTemplate;
        this.wbBaseUrl = wbBaseUrl;
    }

    public WbApiStatusReportDto checkAll() {
        List<WbApiEndpointStatus> statuses = new ArrayList<>();

        // Ключевые ручки, которые реально вызываются приложением
        Map<String, String> endpoints = new LinkedHashMap<>();
        endpoints.put("Ping", absolute("/ping"));
        endpoints.put("Goods (filter)", absolute("/api/v2/list/goods/filter"));
        endpoints.put("Seller Info", "https://common-api.wildberries.ru/api/v1/seller-info");

        // Базовые домены (root) для общей диагностики доступности
        endpoints.put("Content API Root", "https://content-api.wildberries.ru");
        endpoints.put("Statistics API Root", "https://statistics-api.wildberries.ru");
        endpoints.put("Advert API Root", "https://advert-api.wildberries.ru");
        endpoints.put("Finance API Root", "https://finance-api.wildberries.ru");

        for (Map.Entry<String, String> entry : endpoints.entrySet()) {
            statuses.add(checkEndpoint(entry.getKey(), entry.getValue()));
        }

        WbApiStatusReportDto report = new WbApiStatusReportDto();
        report.setCheckedAt(Instant.now());
        report.setEndpoints(statuses);
        return report;
    }

    private String absolute(String path) {
        if (path == null) return wbBaseUrl;
        if (wbBaseUrl.endsWith("/")) {
            return wbBaseUrl.substring(0, wbBaseUrl.length() - 1) + path;
        }
        return wbBaseUrl + path;
    }

    private WbApiEndpointStatus checkEndpoint(String name, String url) {
        try {
            ResponseEntity<String> response = wbRestTemplate.exchange(
                    url, HttpMethod.GET, null, new ParameterizedTypeReference<>(){}
            );
            int code = response.getStatusCode().value();
            String status = code >= 200 && code < 500 && code != 503 ? "UP" : "DOWN";
            return new WbApiEndpointStatus(name, url, status, code, null);
        } catch (RestClientException ex) {
            String message = ex.getMessage() != null ? ex.getMessage() : "request failed";
            String normalized = message.toLowerCase();
            boolean is503 = normalized.contains("503") || normalized.contains("service unavailable");
            return new WbApiEndpointStatus(name, url, is503 ? "DOWN" : "UP", is503 ? 503 : null, message);
        }
    }
}


