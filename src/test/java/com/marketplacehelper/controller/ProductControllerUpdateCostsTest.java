package com.marketplacehelper.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketplacehelper.dto.UpdateCostsRequest;
import com.marketplacehelper.auth.SimpleAuthService;
import com.marketplacehelper.model.Product;
import com.marketplacehelper.service.ProductService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;

@WebMvcTest(ProductController.class)
@AutoConfigureMockMvc(addFilters = false)
class ProductControllerUpdateCostsTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ProductService productService;

    @MockBean
    private SimpleAuthService simpleAuthService;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void patchUpdateCosts_returns200() throws Exception {
        Product p = new Product();
        p.setId(1L);
        p.setName("Test");
        p.setPrice(new BigDecimal("100"));
        given(productService.updateProductCosts(eq(1L), any(), any(), any(), any())).willReturn(p);
        given(simpleAuthService.isTokenValid(anyString())).willReturn(true);

        UpdateCostsRequest body = new UpdateCostsRequest();
        body.setPurchasePrice(new BigDecimal("50"));
        body.setLogisticsCost(new BigDecimal("10"));
        body.setMarketingCost(new BigDecimal("5"));
        body.setOtherExpenses(new BigDecimal("3"));

        mockMvc.perform(patch("/api/products/1/costs")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer test-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk());
    }
}


