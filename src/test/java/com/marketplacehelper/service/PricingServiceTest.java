package com.marketplacehelper.service;

import com.marketplacehelper.dto.AnalyticsReportDto;
import com.marketplacehelper.dto.PriceRecommendationDto;
import com.marketplacehelper.dto.ProductAnalyticsDto;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@SpringBootTest(classes = {PricingService.class})
class PricingServiceTest {

    @Autowired
    private PricingService pricingService;

    @MockBean
    private AnalyticsService analyticsService;

    @Test
    void shouldComputeRecommendedPriceForTargetMargin() {
        ProductAnalyticsDto item = new ProductAnalyticsDto();
        item.setName("SKU");
        item.setWbArticle("100");
        item.setWbPrice(new BigDecimal("1000"));
        item.setPurchasePrice(new BigDecimal("600"));
        item.setLogisticsCost(new BigDecimal("100"));
        item.setMarketingCost(new BigDecimal("50"));
        item.setOtherExpenses(new BigDecimal("50"));

        AnalyticsReportDto report = new AnalyticsReportDto();
        report.setAllItems(List.of(item));
        when(analyticsService.buildProductAnalyticsReport(true, new BigDecimal("20"), true)).thenReturn(report);

        List<PriceRecommendationDto> recs = pricingService.buildRecommendations(new BigDecimal("20"));
        assertThat(recs).hasSize(1);
        PriceRecommendationDto rec = recs.get(0);
        assertThat(rec.getRecommendedPrice()).isEqualByComparingTo("1000.00");
        assertThat(rec.getPriceDelta()).isEqualByComparingTo("0.00");
    }
}


