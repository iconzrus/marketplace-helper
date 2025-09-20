package com.marketplacehelper.service;

import com.marketplacehelper.dto.AlertDto;
import com.marketplacehelper.dto.AnalyticsReportDto;
import com.marketplacehelper.dto.ProductAnalyticsDto;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.TestPropertySource;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@SpringBootTest(classes = {AlertService.class})
@TestPropertySource(properties = {
        "app.alerts.low-stock-threshold=10",
        "app.alerts.margin-percent-threshold=15"
})
class AlertServiceTest {

    @Autowired
    private AlertService alertService;

    @MockBean
    private AnalyticsService analyticsService;

    @Test
    void shouldProduceAlertsForLowMarginNegativeMarginAndLowStock() {
        ProductAnalyticsDto negative = new ProductAnalyticsDto();
        negative.setName("A");
        negative.setWbArticle("111");
        negative.setMargin(new BigDecimal("-10"));
        negative.setMarginPercent(new BigDecimal("-5"));
        negative.setWbStock(100);

        ProductAnalyticsDto lowMargin = new ProductAnalyticsDto();
        lowMargin.setName("B");
        lowMargin.setWbArticle("222");
        lowMargin.setMargin(new BigDecimal("10"));
        lowMargin.setMarginPercent(new BigDecimal("10"));
        lowMargin.setWbStock(100);

        ProductAnalyticsDto lowStock = new ProductAnalyticsDto();
        lowStock.setName("C");
        lowStock.setWbArticle("333");
        lowStock.setMargin(new BigDecimal("50"));
        lowStock.setMarginPercent(new BigDecimal("25"));
        lowStock.setWbStock(5);

        AnalyticsReportDto report = new AnalyticsReportDto();
        report.setAllItems(List.of(negative, lowMargin, lowStock));
        when(analyticsService.buildProductAnalyticsReport(true, new BigDecimal("15"), true)).thenReturn(report);

        List<AlertDto> alerts = alertService.buildAlerts();
        assertThat(alerts).hasSize(3);
        assertThat(alerts.stream().anyMatch(a -> a.getType() == AlertDto.AlertType.NEGATIVE_MARGIN)).isTrue();
        assertThat(alerts.stream().anyMatch(a -> a.getType() == AlertDto.AlertType.LOW_MARGIN)).isTrue();
        assertThat(alerts.stream().anyMatch(a -> a.getType() == AlertDto.AlertType.LOW_STOCK)).isTrue();
    }
}


