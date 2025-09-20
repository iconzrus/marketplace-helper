package com.marketplacehelper.service;

import com.marketplacehelper.dto.AlertDto;
import com.marketplacehelper.dto.AnalyticsReportDto;
import com.marketplacehelper.dto.ProductAnalyticsDto;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Service
public class AlertService {

    private final AnalyticsService analyticsService;
    private final int lowStockThreshold;
    private final BigDecimal marginPercentThreshold;

    public AlertService(AnalyticsService analyticsService,
                        @Value("${app.alerts.low-stock-threshold:10}") int lowStockThreshold,
                        @Value("${app.alerts.margin-percent-threshold:10}") BigDecimal marginPercentThreshold) {
        this.analyticsService = analyticsService;
        this.lowStockThreshold = lowStockThreshold;
        this.marginPercentThreshold = marginPercentThreshold != null ? marginPercentThreshold : BigDecimal.TEN;
    }

    public List<AlertDto> buildAlerts() {
        AnalyticsReportDto analytics = analyticsService.buildProductAnalyticsReport(true, marginPercentThreshold, true);
        List<AlertDto> alerts = new ArrayList<>();
        for (ProductAnalyticsDto dto : analytics.getAllItems()) {
            if (dto.getMargin() != null && dto.getMargin().compareTo(BigDecimal.ZERO) < 0) {
                alerts.add(toAlert(dto, AlertDto.AlertType.NEGATIVE_MARGIN));
            } else if (dto.getMarginPercent() != null && dto.getMarginPercent().compareTo(marginPercentThreshold) < 0) {
                alerts.add(toAlert(dto, AlertDto.AlertType.LOW_MARGIN));
            }
            Integer stock = dto.getWbStock() != null ? dto.getWbStock() : dto.getLocalStock();
            if (stock != null && stock < lowStockThreshold) {
                alerts.add(toAlert(dto, AlertDto.AlertType.LOW_STOCK));
            }
        }
        return alerts;
    }

    private AlertDto toAlert(ProductAnalyticsDto dto, AlertDto.AlertType type) {
        AlertDto alert = new AlertDto();
        alert.setType(type);
        alert.setWbArticle(dto.getWbArticle());
        alert.setName(dto.getName());
        alert.setMargin(dto.getMargin());
        alert.setMarginPercent(dto.getMarginPercent());
        alert.setLocalStock(dto.getLocalStock());
        alert.setWbStock(dto.getWbStock());
        alert.setCurrentPrice(dto.getWbDiscountPrice() != null ? dto.getWbDiscountPrice() : dto.getWbPrice());
        return alert;
    }
}


