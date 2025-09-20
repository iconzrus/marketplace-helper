package com.marketplacehelper.controller;

import com.marketplacehelper.service.AnalyticsService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.SecurityFilterAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = AnalyticsController.class, excludeAutoConfiguration = {
        SecurityAutoConfiguration.class,
        SecurityFilterAutoConfiguration.class
})
@Import(AnalyticsValidationControllerTest.TestConfig.class)
class AnalyticsValidationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AnalyticsService analyticsService;

    static class TestConfig {
        @Bean
        com.marketplacehelper.auth.SimpleAuthService simpleAuthService() {
            return new com.marketplacehelper.auth.SimpleAuthService() {
                @Override
                public boolean isTokenValid(String token) {
                    return true;
                }
            };
        }

        @Bean
        com.marketplacehelper.auth.SimpleAuthFilter simpleAuthFilter(com.marketplacehelper.auth.SimpleAuthService authService) {
            return new com.marketplacehelper.auth.SimpleAuthFilter(authService);
        }
    }

    @Test
    void returnsValidationList() throws Exception {
        when(analyticsService.buildValidationReport(true, null)).thenReturn(List.of());

        mockMvc.perform(get("/api/analytics/validation")
                        .header("Authorization", "Bearer test")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }
}


