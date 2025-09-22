package com.marketplacehelper.controller;

import com.marketplacehelper.dto.PriceRecommendationDto;
import com.marketplacehelper.service.PricingService;
import com.marketplacehelper.dto.BatchPriceUpdateRequest;
import com.marketplacehelper.dto.BatchPriceUpdateResult;
import com.marketplacehelper.service.ProductService;
import com.marketplacehelper.service.WbProductService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api/pricing")
@CrossOrigin(origins = "*")
public class PricingController {

    private final PricingService pricingService;
    private final ProductService productService;
    private final WbProductService wbProductService;

    public PricingController(PricingService pricingService, ProductService productService, WbProductService wbProductService) {
        this.pricingService = pricingService;
        this.productService = productService;
        this.wbProductService = wbProductService;
    }

    @GetMapping("/recommendations")
    public ResponseEntity<List<PriceRecommendationDto>> getRecommendations(
            @RequestParam(name = "targetMarginPercent", required = false) BigDecimal targetMarginPercent
    ) {
        return ResponseEntity.ok(pricingService.buildRecommendations(targetMarginPercent));
    }

    @PostMapping("/batch-update")
    public ResponseEntity<BatchPriceUpdateResult> applyBatchUpdate(@RequestBody BatchPriceUpdateRequest request) {
        BatchPriceUpdateResult result = new BatchPriceUpdateResult();
        if (request == null || request.getItems() == null || request.getItems().isEmpty()) {
            return ResponseEntity.ok(result);
        }
        java.math.BigDecimal maxDeltaPct = request.getMaxDeltaPercent();
        java.math.BigDecimal floor = request.getFloorPrice();
        java.math.BigDecimal ceil = request.getCeilPrice();
        String rounding = request.getRoundingRule();
        boolean dryRun = Boolean.TRUE.equals(request.getDryRun());
        for (BatchPriceUpdateRequest.Item it : request.getItems()) {
            BatchPriceUpdateResult.ResultItem r = new BatchPriceUpdateResult.ResultItem();
            r.setProductId(it.getProductId());
            r.setWbProductId(it.getWbProductId());
            r.setWbArticle(it.getWbArticle());
            try {
                java.math.BigDecimal candidate = it.getNewPrice();
                if (candidate == null) throw new IllegalArgumentException("Не указана новая цена");
                // apply rounding
                candidate = applyRounding(candidate, rounding);
                // bounds
                if (floor != null && candidate.compareTo(floor) < 0) candidate = floor;
                if (ceil != null && candidate.compareTo(ceil) > 0) candidate = ceil;
                // max delta check if current price known
                java.math.BigDecimal current = null;
                if (it.getWbProductId() != null) {
                    current = wbProductService.getWbProductById(it.getWbProductId()).orElseThrow().getPrice();
                } else if (it.getProductId() != null) {
                    current = productService.getProductById(it.getProductId()).orElseThrow().getPrice();
                }
                if (current != null && maxDeltaPct != null && current.compareTo(java.math.BigDecimal.ZERO) > 0) {
                    java.math.BigDecimal diffPct = candidate.subtract(current).abs()
                            .divide(current, 4, java.math.RoundingMode.HALF_UP)
                            .multiply(java.math.BigDecimal.valueOf(100));
                    if (diffPct.compareTo(maxDeltaPct) > 0) {
                        throw new IllegalArgumentException("Изменение превышает допустимый порог " + maxDeltaPct + "%");
                    }
                }

                if (!dryRun) {
                    if (it.getProductId() != null) {
                        productService.updateProductPrice(it.getProductId(), candidate);
                    } else if (it.getWbProductId() != null) {
                        wbProductService.updateWbProductPrice(it.getWbProductId(), candidate);
                    } else {
                        throw new IllegalArgumentException("Не указан идентификатор товара");
                    }
                } else {
                    // no-op, just validation
                }
                r.setSuccess(true);
                r.setMessage(dryRun ? "DRY_RUN" : "OK");
                r.setAppliedPrice(candidate);
                result.setUpdated(result.getUpdated() + 1);
            } catch (Exception ex) {
                r.setSuccess(false);
                r.setMessage(ex.getMessage());
                result.setFailed(result.getFailed() + 1);
            }
            result.getItems().add(r);
        }
        return ResponseEntity.ok(result);
    }

    private java.math.BigDecimal applyRounding(java.math.BigDecimal value, String rule) {
        if (rule == null || rule.isBlank() || "NONE".equalsIgnoreCase(rule)) return value.setScale(2, java.math.RoundingMode.HALF_UP);
        java.math.BigDecimal scaled = value.setScale(0, java.math.RoundingMode.HALF_UP);
        switch (rule) {
            case "NEAREST_1":
                return scaled;
            case "NEAREST_5": {
                long v = scaled.longValue();
                long r = Math.round(v / 5.0) * 5L;
                return java.math.BigDecimal.valueOf(r);
            }
            case "NEAREST_10": {
                long v = scaled.longValue();
                long r = Math.round(v / 10.0) * 10L;
                return java.math.BigDecimal.valueOf(r);
            }
            default:
                return scaled;
        }
    }
}


