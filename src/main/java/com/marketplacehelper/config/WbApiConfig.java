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
    
    @Value("${wb.api.token:}")
    private String wbApiToken;
    
    @Value("${wb.api.base-url:https://marketplace-api.wildberries.ru}")
    private String wbApiBaseUrl;
    
    @Bean
    public RestTemplate wbRestTemplate() {
        RestTemplate restTemplate = new RestTemplate();
        
        // Добавляем интерцептор для автоматической авторизации
        List<ClientHttpRequestInterceptor> interceptors = restTemplate.getInterceptors();
        if (CollectionUtils.isEmpty(interceptors)) {
            interceptors = new ArrayList<>();
        }
        interceptors.add(new WbApiAuthInterceptor(wbApiToken));
        restTemplate.setInterceptors(interceptors);
        
        return restTemplate;
    }
    
    public String getWbApiBaseUrl() {
        return wbApiBaseUrl;
    }
    
    public String getWbApiToken() {
        return wbApiToken;
    }
    
    // Интерцептор для добавления авторизации
    private static class WbApiAuthInterceptor implements ClientHttpRequestInterceptor {
        private final String apiToken;
        
        public WbApiAuthInterceptor(String apiToken) {
            this.apiToken = apiToken;
        }
        
        @Override
        public org.springframework.http.client.ClientHttpResponse intercept(
                org.springframework.http.HttpRequest request,
                byte[] body,
                ClientHttpRequestExecution execution) throws java.io.IOException {
            
            if (apiToken != null && !apiToken.isEmpty()) {
                request.getHeaders().set("Authorization", apiToken);
            }
            
            return execution.execute(request, body);
        }
    }
}
