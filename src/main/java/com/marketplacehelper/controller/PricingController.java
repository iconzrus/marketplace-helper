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
        for (BatchPriceUpdateRequest.Item it : request.getItems()) {
            BatchPriceUpdateResult.ResultItem r = new BatchPriceUpdateResult.ResultItem();
            r.setProductId(it.getProductId());
            r.setWbProductId(it.getWbProductId());
            r.setWbArticle(it.getWbArticle());
            try {
                if (it.getProductId() != null) {
                    productService.updateProductPrice(it.getProductId(), it.getNewPrice());
                } else if (it.getWbProductId() != null) {
                    wbProductService.updateWbProductPrice(it.getWbProductId(), it.getNewPrice());
                } else {
                    throw new IllegalArgumentException("Не указан идентификатор товара");
                }
                r.setSuccess(true);
                r.setMessage("OK");
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
}


