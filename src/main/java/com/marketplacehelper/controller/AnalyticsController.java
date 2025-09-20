package com.marketplacehelper.controller;

import com.marketplacehelper.dto.AnalyticsReportDto;
import com.marketplacehelper.service.AnalyticsService;
import com.marketplacehelper.dto.ProductValidationDto;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@RestController
@RequestMapping("/api/analytics")
@CrossOrigin(origins = "*")
public class AnalyticsController {

    private final AnalyticsService analyticsService;

    public AnalyticsController(AnalyticsService analyticsService) {
        this.analyticsService = analyticsService;
    }

    @GetMapping("/products")
    public ResponseEntity<AnalyticsReportDto> getProductAnalytics(
            @RequestParam(name = "includeWithoutWb", defaultValue = "false") boolean includeWithoutWb,
            @RequestParam(name = "includeUnprofitable", defaultValue = "false") boolean includeUnprofitable,
            @RequestParam(name = "minMarginPercent", required = false) BigDecimal minMarginPercent
    ) {
        return ResponseEntity.ok(analyticsService.buildProductAnalyticsReport(includeWithoutWb, minMarginPercent, includeUnprofitable));
    }

    @GetMapping("/products/export")
    public ResponseEntity<ByteArrayResource> exportProductAnalytics(
            @RequestParam(name = "includeWithoutWb", defaultValue = "false") boolean includeWithoutWb,
            @RequestParam(name = "minMarginPercent", required = false) BigDecimal minMarginPercent
    ) {
        byte[] report = analyticsService.exportProfitableProductsAsCsv(includeWithoutWb, minMarginPercent);
        ByteArrayResource resource = new ByteArrayResource(report);
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss"));
        String filename = String.format("analytics-report-%s.csv", timestamp);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + filename)
                .contentType(MediaType.parseMediaType("text/csv; charset=UTF-8"))
                .contentLength(report.length)
                .body(resource);
    }

    @GetMapping("/validation")
    public ResponseEntity<java.util.List<ProductValidationDto>> getValidation(
            @RequestParam(name = "includeWithoutWb", defaultValue = "true") boolean includeWithoutWb,
            @RequestParam(name = "minMarginPercent", required = false) java.math.BigDecimal minMarginPercent
    ) {
        return ResponseEntity.ok(analyticsService.buildValidationReport(includeWithoutWb, minMarginPercent));
    }
}
