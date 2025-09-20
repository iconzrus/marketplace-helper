package com.marketplacehelper.controller;

import com.marketplacehelper.dto.PriceRecommendationDto;
import com.marketplacehelper.service.PricingService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api/pricing")
@CrossOrigin(origins = "*")
public class PricingController {

    private final PricingService pricingService;

    public PricingController(PricingService pricingService) {
        this.pricingService = pricingService;
    }

    @GetMapping("/recommendations")
    public ResponseEntity<List<PriceRecommendationDto>> getRecommendations(
            @RequestParam(name = "targetMarginPercent", required = false) BigDecimal targetMarginPercent
    ) {
        return ResponseEntity.ok(pricingService.buildRecommendations(targetMarginPercent));
    }
}


