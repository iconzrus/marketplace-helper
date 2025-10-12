package com.marketplacehelper.controller;

import com.marketplacehelper.model.WbProduct;
import com.marketplacehelper.service.WbProductService;
import com.marketplacehelper.service.WbApiService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import com.marketplacehelper.config.WbAuthTokenProvider;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v2")
@CrossOrigin(origins = "*")
public class WbProductController {
    
    private final WbProductService wbProductService;
    private final WbApiService wbApiService;
    private final WbAuthTokenProvider tokenProvider;
    
    @Autowired
    public WbProductController(WbProductService wbProductService, WbApiService wbApiService, WbAuthTokenProvider tokenProvider) {
        this.wbProductService = wbProductService;
        this.wbApiService = wbApiService;
        this.tokenProvider = tokenProvider;
    }
    
    /**
     * Эндпоинт для получения товаров с ценами из реального WB API
     * Эндпоинт: GET /api/v2/list/goods/filter
     */
    @GetMapping("/list/goods/filter")
    public ResponseEntity<?> getGoodsWithPrices(
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String vendor,
            @RequestParam(required = false) String brand,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String subject,
            @RequestParam(required = false) BigDecimal minPrice,
            @RequestParam(required = false) BigDecimal maxPrice,
            @RequestParam(required = false) Integer minDiscount,
            @RequestParam(required = false) Integer lowStockThreshold,
            @RequestParam(required = false) Long filterNmID,
            @RequestParam(required = false, defaultValue = "false") Boolean useLocalData) {
        
        try {
            if (useLocalData) {
                // Используем локальные данные из базы
                List<WbProduct> products = getLocalProducts(name, vendor, brand, category, subject, minPrice, maxPrice, minDiscount, lowStockThreshold);
                return ResponseEntity.ok(products);
            } else {
                // Получаем данные напрямую из WB API
                Map<String, String> filters = new HashMap<>();
                if (name != null) filters.put("name", name);
                if (vendor != null) filters.put("vendor", vendor);
                if (brand != null) filters.put("brand", brand);
                if (category != null) filters.put("category", category);
                if (subject != null) filters.put("subject", subject);
                if (minPrice != null) filters.put("minPrice", minPrice.toString());
                if (maxPrice != null) filters.put("maxPrice", maxPrice.toString());
                if (minDiscount != null) filters.put("minDiscount", minDiscount.toString());
                if (lowStockThreshold != null) filters.put("lowStockThreshold", lowStockThreshold.toString());
                if (filterNmID != null) filters.put("filterNmID", filterNmID.toString());
                
                List<Map<String, Object>> wbProducts = wbApiService.getGoodsWithPricesFiltered(filters);
                return ResponseEntity.ok(wbProducts);
            }
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Ошибка при получении товаров из WB API: " + e.getMessage()));
        }
    }
    
    /**
     * Получение локальных товаров с фильтрацией
     */
    private List<WbProduct> getLocalProducts(String name, String vendor, String brand, String category, 
                                           String subject, BigDecimal minPrice, BigDecimal maxPrice, 
                                           Integer minDiscount, Integer lowStockThreshold) {
        if (name != null && !name.trim().isEmpty()) {
            return wbProductService.searchWbProductsByName(name);
        } else if (vendor != null && !vendor.trim().isEmpty()) {
            return wbProductService.getWbProductsByVendor(vendor);
        } else if (brand != null && !brand.trim().isEmpty()) {
            return wbProductService.getWbProductsByBrand(brand);
        } else if (category != null && !category.trim().isEmpty()) {
            return wbProductService.getWbProductsByCategory(category);
        } else if (subject != null && !subject.trim().isEmpty()) {
            return wbProductService.getWbProductsBySubject(subject);
        } else if (minPrice != null && maxPrice != null) {
            return wbProductService.getWbProductsByPriceRange(minPrice, maxPrice);
        } else if (minDiscount != null) {
            return wbProductService.getWbProductsByDiscount(minDiscount);
        } else if (lowStockThreshold != null) {
            return wbProductService.getLowStockWbProducts(lowStockThreshold);
        } else {
            return wbProductService.getAllWbProducts();
        }
    }
    
    @GetMapping("/wb-products")
    public ResponseEntity<List<WbProduct>> getAllWbProducts() {
        List<WbProduct> products = wbProductService.getAllWbProducts();
        return ResponseEntity.ok(products);
    }
    
    @GetMapping("/wb-products/{id}")
    public ResponseEntity<WbProduct> getWbProductById(@PathVariable Long id) {
        return wbProductService.getWbProductById(id)
                .map(product -> ResponseEntity.ok(product))
                .orElse(ResponseEntity.notFound().build());
    }
    
    @GetMapping("/wb-products/nm/{nmId}")
    public ResponseEntity<WbProduct> getWbProductByNmId(@PathVariable Long nmId) {
        return wbProductService.getWbProductByNmId(nmId)
                .map(product -> ResponseEntity.ok(product))
                .orElse(ResponseEntity.notFound().build());
    }
    
    @PostMapping("/wb-products")
    public ResponseEntity<WbProduct> createWbProduct(@Valid @RequestBody WbProduct wbProduct) {
        WbProduct savedProduct = wbProductService.saveWbProduct(wbProduct);
        return ResponseEntity.status(HttpStatus.CREATED).body(savedProduct);
    }
    
    @PutMapping("/wb-products/{id}")
    public ResponseEntity<WbProduct> updateWbProduct(@PathVariable Long id, @Valid @RequestBody WbProduct wbProductDetails) {
        try {
            WbProduct updatedProduct = wbProductService.updateWbProduct(id, wbProductDetails);
            return ResponseEntity.ok(updatedProduct);
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }
    
    @DeleteMapping("/wb-products/{id}")
    public ResponseEntity<Map<String, String>> deleteWbProduct(@PathVariable Long id) {
        try {
            wbProductService.deleteWbProduct(id);
            return ResponseEntity.ok(Map.of("message", "Товар WB успешно удален"));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Ошибка при удалении товара WB"));
        }
    }
    
    @GetMapping("/wb-products/vendor/{vendor}")
    public ResponseEntity<List<WbProduct>> getWbProductsByVendor(@PathVariable String vendor) {
        List<WbProduct> products = wbProductService.getWbProductsByVendor(vendor);
        return ResponseEntity.ok(products);
    }
    
    @GetMapping("/wb-products/brand/{brand}")
    public ResponseEntity<List<WbProduct>> getWbProductsByBrand(@PathVariable String brand) {
        List<WbProduct> products = wbProductService.getWbProductsByBrand(brand);
        return ResponseEntity.ok(products);
    }
    
    @GetMapping("/wb-products/category/{category}")
    public ResponseEntity<List<WbProduct>> getWbProductsByCategory(@PathVariable String category) {
        List<WbProduct> products = wbProductService.getWbProductsByCategory(category);
        return ResponseEntity.ok(products);
    }
    
    @GetMapping("/wb-products/subject/{subject}")
    public ResponseEntity<List<WbProduct>> getWbProductsBySubject(@PathVariable String subject) {
        List<WbProduct> products = wbProductService.getWbProductsBySubject(subject);
        return ResponseEntity.ok(products);
    }
    
    @GetMapping("/wb-products/search")
    public ResponseEntity<List<WbProduct>> searchWbProducts(@RequestParam String name) {
        List<WbProduct> products = wbProductService.searchWbProductsByName(name);
        return ResponseEntity.ok(products);
    }
    
    @GetMapping("/wb-products/low-stock")
    public ResponseEntity<List<WbProduct>> getLowStockWbProducts(@RequestParam(defaultValue = "10") Integer threshold) {
        List<WbProduct> products = wbProductService.getLowStockWbProducts(threshold);
        return ResponseEntity.ok(products);
    }
    
    @GetMapping("/wb-products/price-range")
    public ResponseEntity<List<WbProduct>> getWbProductsByPriceRange(
            @RequestParam BigDecimal minPrice, 
            @RequestParam BigDecimal maxPrice) {
        List<WbProduct> products = wbProductService.getWbProductsByPriceRange(minPrice, maxPrice);
        return ResponseEntity.ok(products);
    }
    
    @GetMapping("/wb-products/discount")
    public ResponseEntity<List<WbProduct>> getWbProductsByDiscount(@RequestParam Integer minDiscount) {
        List<WbProduct> products = wbProductService.getWbProductsByDiscount(minDiscount);
        return ResponseEntity.ok(products);
    }
    
    // === Эндпоинты для работы с реальным WB API ===
    
    /**
     * Проверка подключения к WB API
     * Эндпоинт: GET /ping
     */
    @GetMapping("/wb-api/ping")
    public ResponseEntity<?> pingWbApi() {
        try {
            Map<String, Object> response = wbApiService.pingWbApi();
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Ошибка при проверке подключения к WB API: " + e.getMessage()));
        }
    }
    
    /**
     * Получение информации о продавце
     * Эндпоинт: GET /api/v1/seller-info
     */
    @GetMapping("/wb-api/seller-info")
    public ResponseEntity<?> getSellerInfo() {
        try {
            Map<String, Object> response = wbApiService.getSellerInfo();
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Ошибка при получении информации о продавце: " + e.getMessage()));
        }
    }

    // Runtime mock mode toggle
    @GetMapping("/wb-api/mock-mode")
    public ResponseEntity<?> getMockMode() {
        return ResponseEntity.ok(Map.of("mock", wbApiService.isMockMode()));
    }

    @PostMapping("/wb-api/mock-mode")
    public ResponseEntity<?> setMockMode(@RequestParam boolean enabled) {
        wbApiService.setMockMode(enabled);
        return ResponseEntity.ok(Map.of("mock", wbApiService.isMockMode()));
    }

    // API token management (runtime)
    @GetMapping("/wb-api/token")
    public ResponseEntity<?> getApiTokenState() {
        String token = tokenProvider.getToken();
        return ResponseEntity.ok(Map.of("hasToken", token != null && !token.isBlank()));
    }

    @PostMapping("/wb-api/token")
    public ResponseEntity<?> setApiToken(@RequestParam String token) {
        tokenProvider.setToken(token);
        return ResponseEntity.ok(Map.of("hasToken", token != null && !token.isBlank()));
    }
    
    /**
     * Синхронизация товаров из WB API в локальную базу данных
     */
    @PostMapping("/wb-api/sync")
    public ResponseEntity<?> syncProductsFromWbApi() {
        try {
            Map<String, Object> stats = wbApiService.syncProductsFromWbApiWithStats();
            return ResponseEntity.ok(stats);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Ошибка при синхронизации товаров: " + e.getMessage()));
        }
    }
    
    /**
     * Получение товаров напрямую из WB API (без фильтрации)
     */
    @GetMapping("/wb-api/goods")
    public ResponseEntity<?> getGoodsFromWbApi() {
        try {
            List<Map<String, Object>> products = wbApiService.getGoodsWithPrices();
            return ResponseEntity.ok(products);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Ошибка при получении товаров из WB API: " + e.getMessage()));
        }
    }

    /**
     * Получить список карточек из Контент API (для подтверждения наличия ассортимента)
     */
    @PostMapping("/wb-api/content/cards")
    public ResponseEntity<?> getContentCards(@RequestParam(required = false) Integer limit,
                                             @RequestParam(required = false) Integer withPhoto,
                                             @RequestParam(required = false) Long nmId,
                                             @RequestParam(required = false) String updatedAt,
                                             @RequestParam(required = false) String locale) {
        try {
            Map<String, Object> result = wbApiService.getContentCardsList(limit, withPhoto, nmId, updatedAt, locale);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Ошибка при получении карточек: " + e.getMessage()));
        }
    }

    @PostMapping("/wb-api/content/cards/trash")
    public ResponseEntity<?> getContentCardsTrash(@RequestParam(required = false) Integer limit,
                                                  @RequestParam(required = false) Integer withPhoto,
                                                  @RequestParam(required = false) Long nmId,
                                                  @RequestParam(required = false) String updatedAt,
                                                  @RequestParam(required = false) String locale) {
        try {
            Map<String, Object> result = wbApiService.getContentCardsTrash(limit, withPhoto, nmId, updatedAt, locale);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Ошибка при получении карточек (trash): " + e.getMessage()));
        }
    }

    @GetMapping("/wb-api/content/cards/limits")
    public ResponseEntity<?> getContentCardsLimits(@RequestParam(required = false) String locale) {
        try {
            Map<String, Object> result = wbApiService.getContentCardsLimits(locale);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Ошибка при получении лимитов: " + e.getMessage()));
        }
    }

    /**
     * Получить цены по списку nmID-ов (через POST /api/v2/list/goods/filter)
     */
    @PostMapping("/wb-api/prices/by-nmids")
    public ResponseEntity<?> getPricesByNmIds(@RequestBody List<Long> nmIds) {
        try {
            List<Map<String, Object>> items = wbApiService.getPricesByNmIds(nmIds);
            return ResponseEntity.ok(items);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Ошибка при получении цен: " + e.getMessage()));
        }
    }
}
