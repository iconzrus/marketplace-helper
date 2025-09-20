package com.marketplacehelper.service;

import com.marketplacehelper.dto.PriceRecommendationDto;
import com.marketplacehelper.dto.AnalyticsReportDto;
import com.marketplacehelper.dto.ProductAnalyticsDto;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

@Service
public class PricingService {

    private final AnalyticsService analyticsService;

    public PricingService(AnalyticsService analyticsService) {
        this.analyticsService = analyticsService;
    }

    public List<PriceRecommendationDto> buildRecommendations(BigDecimal targetMarginPercent) {
        BigDecimal target = targetMarginPercent != null ? targetMarginPercent : BigDecimal.valueOf(15);
        AnalyticsReportDto analytics = analyticsService.buildProductAnalyticsReport(true, target, true);
        List<PriceRecommendationDto> result = new ArrayList<>();
        for (ProductAnalyticsDto item : analytics.getAllItems()) {
            BigDecimal recommended = recommendPrice(item, target);
            if (recommended != null && item.getWbPrice() != null) {
                PriceRecommendationDto dto = new PriceRecommendationDto();
                dto.setWbArticle(item.getWbArticle());
                dto.setName(item.getName());
                dto.setCurrentPrice(item.getWbPrice());
                dto.setTargetMarginPercent(target);
                dto.setRecommendedPrice(recommended);
                dto.setPriceDelta(recommended.subtract(item.getWbPrice()).setScale(2, RoundingMode.HALF_UP));
                result.add(dto);
            }
        }
        return result;
    }

    private BigDecimal recommendPrice(ProductAnalyticsDto item, BigDecimal targetMarginPercent) {
        BigDecimal totalCosts = sum(item.getPurchasePrice(), item.getLogisticsCost(), item.getMarketingCost(), item.getOtherExpenses());
        if (totalCosts == null) {
            return null;
        }
        BigDecimal target = targetMarginPercent.divide(BigDecimal.valueOf(100), 4, RoundingMode.HALF_UP);
        BigDecimal price = totalCosts.divide(BigDecimal.ONE.subtract(target), 4, RoundingMode.HALF_UP);
        if (price.compareTo(BigDecimal.ZERO) <= 0) {
            return null;
        }
        return price.setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal sum(BigDecimal... values) {
        BigDecimal result = BigDecimal.ZERO;
        boolean has = false;
        for (BigDecimal v : values) {
            if (v != null) {
                result = result.add(v);
                has = true;
            }
        }
        return has ? result : null;
    }
}


