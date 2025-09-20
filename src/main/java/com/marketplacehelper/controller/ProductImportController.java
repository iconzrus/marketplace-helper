package com.marketplacehelper.controller;

import com.marketplacehelper.model.Product;
import com.marketplacehelper.service.ProductImportService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/products/import")
@CrossOrigin(origins = "*")
public class ProductImportController {

    private final ProductImportService productImportService;

    public ProductImportController(ProductImportService productImportService) {
        this.productImportService = productImportService;
    }

    @PostMapping("/excel")
    public ResponseEntity<?> importExcel(@RequestParam("file") MultipartFile file) {
        try {
            List<Product> products = productImportService.importFromExcel(file);
            return ResponseEntity.status(HttpStatus.CREATED).body(products);
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(Map.of("error", ex.getMessage()));
        } catch (Exception ex) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Ошибка при импорте файла: " + ex.getMessage()));
        }
    }
}
