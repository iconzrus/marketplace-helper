package com.marketplacehelper.controller;

import com.marketplacehelper.dto.WbApiEndpointStatus;
import com.marketplacehelper.dto.WbApiStatusReportDto;
import com.marketplacehelper.service.WbApiStatusService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.SecurityFilterAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = WbApiStatusController.class, excludeAutoConfiguration = {
        SecurityAutoConfiguration.class,
        SecurityFilterAutoConfiguration.class
})
@Import(WbApiStatusControllerTest.TestConfig.class)
class WbApiStatusControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private WbApiStatusService statusService;

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
    void returnsStatusReport() throws Exception {
        WbApiStatusReportDto report = new WbApiStatusReportDto();
        report.setCheckedAt(Instant.now());
        report.setEndpoints(List.of(new WbApiEndpointStatus("Ping", "https://marketplace-api.wildberries.ru/ping", "UP", 200, null)));
        when(statusService.checkAll()).thenReturn(report);

        mockMvc.perform(get("/api/wb-status").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.endpoints[0].name").value("Ping"))
                .andExpect(jsonPath("$.endpoints[0].status").value("UP"));
    }
}


