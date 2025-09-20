package com.marketplacehelper.controller;

import com.marketplacehelper.dto.ProductAnalyticsDto;
import com.marketplacehelper.service.AnalyticsService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/analytics")
@CrossOrigin(origins = "*")
public class AnalyticsController {

    private final AnalyticsService analyticsService;

    public AnalyticsController(AnalyticsService analyticsService) {
        this.analyticsService = analyticsService;
    }

    @GetMapping("/products")
    public ResponseEntity<List<ProductAnalyticsDto>> getProductAnalytics(
            @RequestParam(name = "includeWithoutWb", defaultValue = "false") boolean includeWithoutWb
    ) {
        return ResponseEntity.ok(analyticsService.buildProductAnalytics(includeWithoutWb));
    }
}
