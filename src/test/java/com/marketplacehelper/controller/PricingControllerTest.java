package com.marketplacehelper.controller;

import com.marketplacehelper.dto.PriceRecommendationDto;
import com.marketplacehelper.service.PricingService;
import com.marketplacehelper.service.ProductService;
import com.marketplacehelper.service.WbProductService;
import com.marketplacehelper.auth.SimpleAuthFilter;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

@WebMvcTest(controllers = PricingController.class,
        excludeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = SimpleAuthFilter.class))
class PricingControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private PricingService pricingService;

    @MockBean
    private ProductService productService;

    @MockBean
    private WbProductService wbProductService;

    // exclude SimpleAuthFilter via annotation above

    @Test
    void shouldReturnRecommendations() throws Exception {
        PriceRecommendationDto dto = new PriceRecommendationDto();
        dto.setWbArticle("10");
        dto.setName("Test");
        dto.setCurrentPrice(new BigDecimal("100"));
        dto.setTargetMarginPercent(new BigDecimal("20"));
        dto.setRecommendedPrice(new BigDecimal("125"));
        dto.setPriceDelta(new BigDecimal("25"));
        when(pricingService.buildRecommendations(any())).thenReturn(List.of(dto));

        mockMvc.perform(get("/api/pricing/recommendations").param("targetMarginPercent", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].wbArticle").value("10"))
                .andExpect(jsonPath("$[0].recommendedPrice").value(125));
    }

    @Test
    void batchUpdate_shouldAcceptEmpty() throws Exception {
        mockMvc.perform(post("/api/pricing/batch-update")
                        .contentType("application/json")
                        .content("{\"items\":[]}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.updated").value(0));
    }

    @Test
    void batchUpdate_shouldApplyConstraintsAndDryRun() throws Exception {
        // current price = 100
        com.marketplacehelper.model.WbProduct wb = new com.marketplacehelper.model.WbProduct();
        wb.setId(1L);
        wb.setPrice(new java.math.BigDecimal("100"));
        when(wbProductService.getWbProductById(1L)).thenReturn(java.util.Optional.of(wb));

        String payload = "{" +
                "\"dryRun\":true," +
                "\"roundingRule\":\"NEAREST_10\"," +
                "\"floorPrice\":90," +
                "\"ceilPrice\":150," +
                "\"maxDeltaPercent\":20," +
                "\"items\":[{" +
                "\"wbProductId\":1," +
                "\"newPrice\":111" +
                "}]" +
                "}";

        mockMvc.perform(post("/api/pricing/batch-update")
                        .contentType("application/json")
                        .content(payload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].success").value(true))
                .andExpect(jsonPath("$.items[0].message").value("DRY_RUN"))
                // 111 with NEAREST_10 -> 110
                .andExpect(jsonPath("$.items[0].appliedPrice").value(110));
    }

    @Test
    void batchUpdate_shouldRejectTooLargeDelta() throws Exception {
        com.marketplacehelper.model.WbProduct wb = new com.marketplacehelper.model.WbProduct();
        wb.setId(2L);
        wb.setPrice(new java.math.BigDecimal("100"));
        when(wbProductService.getWbProductById(2L)).thenReturn(java.util.Optional.of(wb));

        String payload = "{" +
                "\"maxDeltaPercent\":10," +
                "\"items\":[{" +
                "\"wbProductId\":2," +
                "\"newPrice\":200" + // 100% delta
                "}]" +
                "}";

        mockMvc.perform(post("/api/pricing/batch-update")
                        .contentType("application/json")
                        .content(payload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.failed").value(1))
                .andExpect(jsonPath("$.items[0].success").value(false));
    }
}


