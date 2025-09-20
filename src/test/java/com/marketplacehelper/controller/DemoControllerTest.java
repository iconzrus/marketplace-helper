package com.marketplacehelper.controller;

import com.marketplacehelper.dto.AutoFillRequestDto;
import com.marketplacehelper.dto.AutoFillResultDto;
import com.marketplacehelper.service.DemoDataService;
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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = DemoController.class, excludeAutoConfiguration = {
        SecurityAutoConfiguration.class,
        SecurityFilterAutoConfiguration.class
})
@Import(DemoControllerTest.TestConfig.class)
class DemoControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private DemoDataService demoDataService;

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
    void autofillEndpointReturnsResult() throws Exception {
        AutoFillResultDto dto = new AutoFillResultDto();
        dto.setAffectedCount(5);
        when(demoDataService.autoFillMissingCosts(any(AutoFillRequestDto.class))).thenReturn(dto);

        mockMvc.perform(post("/api/demo/autofill-costs")
                        .header("Authorization", "Bearer test")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"purchasePercentOfPrice\":60,\"logisticsFixed\":70,\"marketingPercentOfPrice\":8,\"otherFixed\":0}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.affectedCount").value(5));
    }

    @Test
    void fillRandomAllReturnsAffected() throws Exception {
        when(demoDataService.fillRandomAll()).thenReturn(42);

        mockMvc.perform(post("/api/demo/fill-random-all")
                        .header("Authorization", "Bearer test"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.affected").value(42));
    }

    @Test
    void generateEndpointReturnsCreated() throws Exception {
        when(demoDataService.generateDemo(10, "both")).thenReturn(10);
        mockMvc.perform(post("/api/demo/generate?count=10&type=both")
                        .header("Authorization", "Bearer test"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.created").value(10));
    }
}


