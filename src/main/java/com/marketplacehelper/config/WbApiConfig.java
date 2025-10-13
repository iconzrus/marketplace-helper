package com.marketplacehelper.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.HttpHeaders;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
import java.util.List;

@Configuration
public class WbApiConfig {
    
    private final WbAuthTokenProvider tokenProvider;
    
    @Value("${wb.api.base-url:https://marketplace-api.wildberries.ru}")
    private String wbApiBaseUrl;
    
    public WbApiConfig(WbAuthTokenProvider tokenProvider) {
        this.tokenProvider = tokenProvider;
    }

    @Bean
    public RestTemplate wbRestTemplate() {
        RestTemplate restTemplate = new RestTemplate();
        
        // Добавляем интерцептор для автоматической авторизации
        List<ClientHttpRequestInterceptor> interceptors = restTemplate.getInterceptors();
        if (CollectionUtils.isEmpty(interceptors)) {
            interceptors = new ArrayList<>();
        }
        interceptors.add(new WbApiAuthInterceptor(tokenProvider));
        restTemplate.setInterceptors(interceptors);
        
        return restTemplate;
    }
    
    public String getWbApiBaseUrl() {
        return wbApiBaseUrl;
    }
    
    public String getWbApiToken() { return tokenProvider.getToken(); }
    
    // Интерцептор для добавления авторизации
    private static class WbApiAuthInterceptor implements ClientHttpRequestInterceptor {
        private final WbAuthTokenProvider tokenProvider;
        public WbApiAuthInterceptor(WbAuthTokenProvider tokenProvider) { this.tokenProvider = tokenProvider; }
        
        @Override
        public org.springframework.http.client.ClientHttpResponse intercept(
                org.springframework.http.HttpRequest request,
                byte[] body,
                ClientHttpRequestExecution execution) throws java.io.IOException {
            
            String apiToken = tokenProvider.getToken();
            // WB ожидает токен БЕЗ префикса Bearer для большинства API (Discounts/Prices, Content, Common)
            if (apiToken != null && !apiToken.isBlank()) {
                request.getHeaders().set("Authorization", apiToken);
            }

            // Базовые заголовки
            if (!request.getHeaders().containsKey(HttpHeaders.ACCEPT)) {
                request.getHeaders().set(HttpHeaders.ACCEPT, "application/json");
            }
            if (!request.getHeaders().containsKey(HttpHeaders.USER_AGENT)) {
                request.getHeaders().set(HttpHeaders.USER_AGENT, "marketplace-helper/0.0.1");
            }
            // Если это POST/PUT и не указан Content-Type – проставим JSON
            if (request.getMethod() != null && (request.getMethod().name().equals("POST") || request.getMethod().name().equals("PUT"))) {
                if (!request.getHeaders().containsKey(HttpHeaders.CONTENT_TYPE)) {
                    request.getHeaders().set(HttpHeaders.CONTENT_TYPE, "application/json");
                }
            }
            
            return execution.execute(request, body);
        }
    }
}
