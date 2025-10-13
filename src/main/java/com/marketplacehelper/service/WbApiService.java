package com.marketplacehelper.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketplacehelper.config.WbApiConfig;
import com.marketplacehelper.model.WbProduct;
import com.marketplacehelper.repository.WbProductRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class WbApiService {

    private final RestTemplate wbRestTemplate;
    private final WbApiConfig wbApiConfig;
    private final WbProductRepository wbProductRepository;
    private final ObjectMapper objectMapper;
    private final ResourceLoader resourceLoader;
    private final boolean mockMode;
    private final String mockDataPath;

    private List<Map<String, Object>> cachedMockProducts;
    private volatile boolean runtimeMockMode;
    private volatile String mockSellerCompany = null;
    private volatile String mockSellerInn = null;

    public WbApiService(RestTemplate wbRestTemplate,
                        WbApiConfig wbApiConfig,
                        WbProductRepository wbProductRepository,
                        ObjectMapper objectMapper,
                        ResourceLoader resourceLoader,
                        @Value("${wb.api.mock-mode:false}") boolean mockMode,
                        @Value("${wb.api.mock-data-path:}") String mockDataPath) {
        this.wbRestTemplate = wbRestTemplate;
        this.wbApiConfig = wbApiConfig;
        this.wbProductRepository = wbProductRepository;
        this.objectMapper = objectMapper;
        this.resourceLoader = resourceLoader;
        this.mockMode = mockMode;
        this.mockDataPath = mockDataPath;
        this.runtimeMockMode = mockMode;
    }

    public List<Map<String, Object>> getGoodsWithPrices() {
        if (shouldUseMock()) {
            return loadMockProducts();
        }

        try {
            String url = wbApiConfig.getWbApiBaseUrl() + "/api/v2/list/goods/filter?limit=1000&offset=0";
            ResponseEntity<Map<String, Object>> response = wbRestTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    null,
                    new ParameterizedTypeReference<Map<String, Object>>() {}
            );

            Map<String, Object> body = response.getBody();
            if (body == null) {
                return Collections.emptyList();
            }
            Object data = body.get("data");
            // New shape: { "data": { "listGoods": [...] }, ... }
            if (data instanceof Map<?, ?> m) {
                Object lg = ((Map<?, ?>) m).get("listGoods");
                if (lg instanceof List<?> list) {
                    @SuppressWarnings("unchecked")
                    List<Map<String, Object>> casted = (List<Map<String, Object>>) (List<?>) list;
                    return casted.stream().map(this::normalizeDiscountsPricesItem).collect(Collectors.toList());
                }
            }
            if (data instanceof List<?> list) {
                @SuppressWarnings("unchecked")
                List<Map<String, Object>> casted = (List<Map<String, Object>>) (List<?>) list;
                return casted.stream().map(this::normalizeDiscountsPricesItem).collect(Collectors.toList());
            }
            return Collections.emptyList();
        } catch (Exception e) {
            throw new RuntimeException("Ошибка при получении товаров из WB API: " + e.getMessage(), e);
        }
    }

    public List<Map<String, Object>> getGoodsWithPricesFiltered(Map<String, String> filters) {
        if (shouldUseMock()) {
            List<Map<String, Object>> products = loadMockProducts();
            if (filters == null || filters.isEmpty()) {
                return products;
            }
            return products.stream()
                    .filter(product -> matchesFilters(product, filters))
                    .collect(Collectors.toList());
        }

        try {
            StringBuilder urlBuilder = new StringBuilder(wbApiConfig.getWbApiBaseUrl() + "/api/v2/list/goods/filter?");
            String limit = filters != null && filters.get("limit") != null ? filters.get("limit") : "1000";
            String offset = filters != null && filters.get("offset") != null ? filters.get("offset") : "0";
            urlBuilder.append("limit=").append(limit).append("&offset=").append(offset);

            if (filters != null && !filters.isEmpty()) {
                for (Map.Entry<String, String> entry : filters.entrySet()) {
                    String key = entry.getKey();
                    if ("limit".equalsIgnoreCase(key) || "offset".equalsIgnoreCase(key)) {
                        continue;
                    }
                    urlBuilder.append("&").append(key).append("=").append(entry.getValue());
                }
            }
            ResponseEntity<Map<String, Object>> response = wbRestTemplate.exchange(
                    urlBuilder.toString(),
                    HttpMethod.GET,
                    null,
                    new ParameterizedTypeReference<Map<String, Object>>() {}
            );
            Map<String, Object> body = response.getBody();
            if (body == null) {
                return Collections.emptyList();
            }
            Object data = body.get("data");
            if (data instanceof Map<?, ?> m) {
                Object lg = ((Map<?, ?>) m).get("listGoods");
                if (lg instanceof List<?> list) {
                    @SuppressWarnings("unchecked")
                    List<Map<String, Object>> casted = (List<Map<String, Object>>) (List<?>) list;
                    return casted.stream().map(this::normalizeDiscountsPricesItem).collect(Collectors.toList());
                }
            }
            if (data instanceof List<?> list) {
                @SuppressWarnings("unchecked")
                List<Map<String, Object>> casted = (List<Map<String, Object>>) (List<?>) list;
                return casted.stream().map(this::normalizeDiscountsPricesItem).collect(Collectors.toList());
            }
            return Collections.emptyList();
        } catch (Exception e) {
            throw new RuntimeException("Ошибка при получении товаров из WB API с фильтрацией: " + e.getMessage(), e);
        }
    }

    public Map<String, Object> pingWbApi() {
        if (shouldUseMock()) {
            Map<String, Object> response = new LinkedHashMap<>();
            response.put("mock", true);
            response.put("message", "Используется демонстрационная заглушка WB API");
            response.put("timestamp", Instant.now().toString());
            response.put("items", loadMockProducts().size());
            return response;
        }

        try {
            String url = wbApiConfig.getWbApiBaseUrl() + "/ping";
            ResponseEntity<Map<String, Object>> response = wbRestTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    null,
                    new ParameterizedTypeReference<Map<String, Object>>() {}
            );
            return response.getBody();
        } catch (Exception e) {
            throw new RuntimeException("Ошибка при проверке подключения к WB API: " + e.getMessage(), e);
        }
    }

    public Map<String, Object> getSellerInfo() {
        if (shouldUseMock()) {
            // Only return seller info if it was explicitly generated
            if (mockSellerCompany == null || mockSellerInn == null) {
                // Return empty mock response - no seller generated yet
                Map<String, Object> response = new LinkedHashMap<>();
                response.put("mock", true);
                response.put("message", "Сгенерируйте кабинет для создания продавца");
                return response;
            }
            Map<String, Object> response = new LinkedHashMap<>();
            response.put("company", mockSellerCompany);
            response.put("inn", mockSellerInn);
            response.put("mock", true);
            response.put("updatedAt", Instant.now().toString());
            return response;
        }

        try {
            String url = "https://common-api.wildberries.ru/api/v1/seller-info";
            ResponseEntity<Map<String, Object>> response = wbRestTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    null,
                    new ParameterizedTypeReference<Map<String, Object>>() {}
            );
            return response.getBody();
        } catch (Exception e) {
            throw new RuntimeException("Ошибка при получении информации о продавце: " + e.getMessage(), e);
        }
    }
    
    public void regenerateMockSeller() {
        mockSellerCompany = randomCompany();
        mockSellerInn = String.valueOf(7700000000L + new java.util.Random().nextInt(99999999));
    }

    private String randomCompany() {
        String[] forms = {"ИП", "ООО", "АО"};
        String[] names = {"Север", "Вектор", "Атлант", "Прометей", "Сфера", "Гермес"};
        java.util.Random r = new java.util.Random();
        String form = forms[r.nextInt(forms.length)];
        String name = names[r.nextInt(names.length)];
        String surname = new String[]{"Смирнов", "Иванов", "Петров", "Соколов", "Кузнецов"}[r.nextInt(5)];
        if ("ИП".equals(form)) {
            return form + " " + surname + " " + name.charAt(0) + ". ";
        }
        return form + " \"" + name + "\"";
    }

    public Map<String, Object> getContentCardsList(Integer limit,
                                                   Integer withPhoto,
                                                   Long nmId,
                                                   String updatedAt,
                                                   String locale) {
        if (shouldUseMock()) {
            Map<String, Object> response = new LinkedHashMap<>();
            response.put("mock", true);
            response.put("message", "Контент API не вызывается в mock-режиме");
            response.put("data", List.of());
            return response;
        }

        try {
            StringBuilder url = new StringBuilder("https://content-api.wildberries.ru/content/v2/get/cards/list");
            if (locale != null && !locale.isBlank()) {
                url.append("?locale=").append(locale);
            }

            // Content API ожидает структуру settings: { filter: {...}, cursor: {...}, sort?: {...} }
            Map<String, Object> cursor = new LinkedHashMap<>();
            cursor.put("limit", limit == null ? 100 : limit);
            if (nmId != null) {
                cursor.put("nmID", nmId);
            }
            if (updatedAt != null && !updatedAt.isBlank()) {
                cursor.put("updatedAt", updatedAt);
            }

            Map<String, Object> filter = new LinkedHashMap<>();
            if (withPhoto != null) {
                // -1: любые, 0: без фото, 1: с фото
                filter.put("withPhoto", withPhoto);
            }

            // сортировка опциональна, опускаем для совместимости

            Map<String, Object> settings = new LinkedHashMap<>();
            settings.put("filter", filter);
            settings.put("cursor", cursor);

            Map<String, Object> body = new LinkedHashMap<>();
            body.put("settings", settings);

            org.springframework.http.HttpHeaders headers = new org.springframework.http.HttpHeaders();
            headers.set("Content-Type", "application/json");
            headers.set("Accept", "application/json");
            org.springframework.http.HttpEntity<Map<String, Object>> entity =
                    new org.springframework.http.HttpEntity<>(body, headers);

            ResponseEntity<Map<String, Object>> response = wbRestTemplate.exchange(
                    url.toString(),
                    HttpMethod.POST,
                    entity,
                    new ParameterizedTypeReference<Map<String, Object>>() {}
            );
            return response.getBody();
        } catch (org.springframework.web.client.RestClientResponseException httpEx) {
            String details = httpEx.getResponseBodyAsString();
            throw new RuntimeException("Ошибка при получении карточек из Контент API: " + httpEx.getStatusCode() + ": " + details, httpEx);
        } catch (Exception e) {
            throw new RuntimeException("Ошибка при получении карточек из Контент API: " + e.getMessage(), e);
        }
    }

    public Map<String, Object> getContentCardsTrash(Integer limit,
                                                    Integer withPhoto,
                                                    Long nmId,
                                                    String updatedAt,
                                                    String locale) {
        if (shouldUseMock()) {
            Map<String, Object> response = new LinkedHashMap<>();
            response.put("mock", true);
            response.put("message", "Контент API (trash) не вызывается в mock-режиме");
            response.put("cards", List.of());
            response.put("cursor", Map.of("nmID", 0, "total", 0));
            return response;
        }

        try {
            StringBuilder url = new StringBuilder("https://content-api.wildberries.ru/content/v2/get/cards/trash");
            if (locale != null && !locale.isBlank()) {
                url.append("?locale=").append(locale);
            }

            Map<String, Object> cursor = new LinkedHashMap<>();
            cursor.put("limit", limit == null ? 100 : limit);
            if (nmId != null) {
                cursor.put("nmID", nmId);
            }
            if (updatedAt != null && !updatedAt.isBlank()) {
                cursor.put("updatedAt", updatedAt);
            }

            Map<String, Object> filter = new LinkedHashMap<>();
            if (withPhoto != null) {
                filter.put("withPhoto", withPhoto);
            }

            Map<String, Object> settings = new LinkedHashMap<>();
            settings.put("filter", filter);
            settings.put("cursor", cursor);

            Map<String, Object> body = new LinkedHashMap<>();
            body.put("settings", settings);

            org.springframework.http.HttpHeaders headers = new org.springframework.http.HttpHeaders();
            headers.set("Content-Type", "application/json");
            headers.set("Accept", "application/json");
            org.springframework.http.HttpEntity<Map<String, Object>> entity =
                    new org.springframework.http.HttpEntity<>(body, headers);

            ResponseEntity<Map<String, Object>> response = wbRestTemplate.exchange(
                    url.toString(),
                    HttpMethod.POST,
                    entity,
                    new ParameterizedTypeReference<Map<String, Object>>() {}
            );
            return response.getBody();
        } catch (org.springframework.web.client.RestClientResponseException httpEx) {
            String details = httpEx.getResponseBodyAsString();
            throw new RuntimeException("Ошибка при получении карточек (trash) из Контент API: " + httpEx.getStatusCode() + ": " + details, httpEx);
        } catch (Exception e) {
            throw new RuntimeException("Ошибка при получении карточек (trash) из Контент API: " + e.getMessage(), e);
        }
    }

    public Map<String, Object> getContentCardsLimits(String locale) {
        if (shouldUseMock()) {
            return Map.of("mock", true, "limits", Map.of());
        }
        try {
            StringBuilder url = new StringBuilder("https://content-api.wildberries.ru/content/v2/cards/limits");
            if (locale != null && !locale.isBlank()) {
                url.append("?locale=").append(locale);
            }
            ResponseEntity<Map<String, Object>> response = wbRestTemplate.exchange(
                    url.toString(),
                    HttpMethod.GET,
                    null,
                    new ParameterizedTypeReference<Map<String, Object>>() {}
            );
            return response.getBody();
        } catch (org.springframework.web.client.RestClientResponseException httpEx) {
            String details = httpEx.getResponseBodyAsString();
            throw new RuntimeException("Ошибка при получении лимитов Контент API: " + httpEx.getStatusCode() + ": " + details, httpEx);
        } catch (Exception e) {
            throw new RuntimeException("Ошибка при получении лимитов Контент API: " + e.getMessage(), e);
        }
    }

    public List<Map<String, Object>> getPricesByNmIds(List<Long> nmIds) {
        if (shouldUseMock()) {
            return loadMockProducts();
        }
        if (nmIds == null || nmIds.isEmpty()) {
            return Collections.emptyList();
        }
        try {
            String url = wbApiConfig.getWbApiBaseUrl() + "/api/v2/list/goods/filter";
            Map<String, Object> body = new LinkedHashMap<>();
            body.put("nmIDs", nmIds);
            org.springframework.http.HttpEntity<Map<String, Object>> entity =
                    new org.springframework.http.HttpEntity<>(body);

            ResponseEntity<Map<String, Object>> response = wbRestTemplate.exchange(
                    url,
                    HttpMethod.POST,
                    entity,
                    new ParameterizedTypeReference<Map<String, Object>>() {}
            );
            Map<String, Object> map = response.getBody();
            if (map == null) return Collections.emptyList();
            Object data = map.get("data");
            if (data instanceof List<?> list) {
                @SuppressWarnings("unchecked")
                List<Map<String, Object>> casted = (List<Map<String, Object>>) (List<?>) list;
                return casted;
            }
            return Collections.emptyList();
        } catch (Exception e) {
            throw new RuntimeException("Ошибка при получении цен по nmIDs: " + e.getMessage(), e);
        }
    }
    public void syncProductsFromWbApi() {
        try {
            List<Map<String, Object>> wbProducts = getGoodsWithPrices();
            for (Map<String, Object> productData : wbProducts) {
                WbProduct product = convertToWbProduct(productData);

                Optional<WbProduct> existing = Optional.empty();
                if (product.getNmId() != null) {
                    existing = wbProductRepository.findByNmId(product.getNmId());
                }
                if (existing.isEmpty() && product.getVendorCode() != null) {
                    existing = wbProductRepository.findByVendorCode(product.getVendorCode());
                }

                existing.ifPresentOrElse(existingProduct -> {
                            updateWbProductFromData(existingProduct, productData);
                            wbProductRepository.save(existingProduct);
                        },
                        () -> wbProductRepository.save(product));
            }
        } catch (Exception e) {
            throw new RuntimeException("Ошибка при синхронизации товаров: " + e.getMessage(), e);
        }
    }

    public Map<String, Object> syncProductsFromWbApiWithStats() {
        try {
            List<Map<String, Object>> wbProducts = getGoodsWithPrices();
            int fetched = wbProducts == null ? 0 : wbProducts.size();
            int inserted = 0;
            int updated = 0;
            int skipped = 0;

            if (wbProducts != null) {
                for (Map<String, Object> productData : wbProducts) {
                    WbProduct product = convertToWbProduct(productData);

                    Optional<WbProduct> existing = Optional.empty();
                    if (product.getNmId() != null) {
                        existing = wbProductRepository.findByNmId(product.getNmId());
                    }
                    if (existing.isEmpty() && product.getVendorCode() != null) {
                        existing = wbProductRepository.findByVendorCode(product.getVendorCode());
                    }

                    if (existing.isPresent()) {
                        WbProduct existingProduct = existing.get();
                        updateWbProductFromData(existingProduct, productData);
                        wbProductRepository.save(existingProduct);
                        updated++;
                    } else if (product.getNmId() != null || product.getVendorCode() != null) {
                        wbProductRepository.save(product);
                        inserted++;
                    } else {
                        skipped++;
                    }
                }
            }

            Map<String, Object> result = new LinkedHashMap<>();
            result.put("fetched", fetched);
            result.put("inserted", inserted);
            result.put("updated", updated);
            result.put("upserted", inserted + updated);
            result.put("skipped", skipped);
            result.put("message", fetched == 0 ? "WB вернул 0 товаров" : "Синхронизация завершена");
            return result;
        } catch (Exception e) {
            throw new RuntimeException("Ошибка при синхронизации товаров: " + e.getMessage(), e);
        }
    }

    public boolean isMockMode() {
        return runtimeMockMode;
    }

    public void setMockMode(boolean enabled) {
        this.runtimeMockMode = enabled;
    }

    private boolean shouldUseMock() {
        return runtimeMockMode;
    }

    private List<Map<String, Object>> loadMockProducts() {
        if (cachedMockProducts != null) {
            return cachedMockProducts;
        }
        if (mockDataPath == null || mockDataPath.isBlank()) {
            throw new IllegalStateException("Не указан путь к файлу с данными заглушки WB API");
        }
        Resource resource = resourceLoader.getResource(mockDataPath);
        if (!resource.exists()) {
            throw new IllegalStateException("Файл с данными заглушки не найден: " + mockDataPath);
        }
        try (InputStream inputStream = resource.getInputStream()) {
            cachedMockProducts = objectMapper.readValue(inputStream, new TypeReference<>() {});
            return cachedMockProducts;
        } catch (IOException e) {
            throw new IllegalStateException("Не удалось прочитать файл заглушки WB API: " + e.getMessage(), e);
        }
    }

    private boolean matchesFilters(Map<String, Object> product, Map<String, String> filters) {
        if (filters.containsKey("name") && !containsIgnoreCase(product, "name", filters.get("name"))) {
            return false;
        }
        if (filters.containsKey("vendor") && !containsIgnoreCase(product, "vendor", filters.get("vendor"))) {
            return false;
        }
        if (filters.containsKey("brand") && !containsIgnoreCase(product, "brand", filters.get("brand"))) {
            return false;
        }
        if (filters.containsKey("category") && !containsIgnoreCase(product, "category", filters.get("category"))) {
            return false;
        }
        if (filters.containsKey("subject") && !containsIgnoreCase(product, "subject", filters.get("subject"))) {
            return false;
        }

        BigDecimal price = getBigDecimal(product, "price");
        if (filters.containsKey("minPrice")) {
            BigDecimal minPrice = parseDecimal(filters.get("minPrice"));
            if (minPrice != null && (price == null || price.compareTo(minPrice) < 0)) {
                return false;
            }
        }
        if (filters.containsKey("maxPrice")) {
            BigDecimal maxPrice = parseDecimal(filters.get("maxPrice"));
            if (maxPrice != null && (price == null || price.compareTo(maxPrice) > 0)) {
                return false;
            }
        }
        if (filters.containsKey("minDiscount")) {
            Integer discount = getInteger(product, "discount");
            Integer minDiscount = parseInteger(filters.get("minDiscount"));
            if (minDiscount != null && (discount == null || discount < minDiscount)) {
                return false;
            }
        }
        if (filters.containsKey("lowStockThreshold")) {
            Integer stock = getInteger(product, "total_quantity");
            Integer threshold = parseInteger(filters.get("lowStockThreshold"));
            if (threshold != null && (stock == null || stock >= threshold)) {
                return false;
            }
        }
        return true;
    }

    private boolean containsIgnoreCase(Map<String, Object> product, String key, String value) {
        if (value == null || value.isBlank()) {
            return true;
        }
        Object field = product.get(key);
        return field != null && field.toString().toLowerCase(Locale.ROOT).contains(value.toLowerCase(Locale.ROOT));
    }

    private BigDecimal getBigDecimal(Map<String, Object> product, String key) {
        Object value = product.get(key);
        if (value == null) {
            return null;
        }
        if (value instanceof Number number) {
            return BigDecimal.valueOf(number.doubleValue());
        }
        try {
            return new BigDecimal(value.toString());
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private Integer getInteger(Map<String, Object> product, String key) {
        Object value = product.get(key);
        if (value == null) {
            return null;
        }
        if (value instanceof Number number) {
            return number.intValue();
        }
        try {
            return Integer.parseInt(value.toString());
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private BigDecimal parseDecimal(String value) {
        if (value == null) {
            return null;
        }
        try {
            return new BigDecimal(value);
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private Integer parseInteger(String value) {
        if (value == null) {
            return null;
        }
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private WbProduct convertToWbProduct(Map<String, Object> data) {
        WbProduct product = new WbProduct();
        if (data.get("nm_id") != null) {
            product.setNmId(Long.valueOf(data.get("nm_id").toString()));
        } else if (data.get("nmID") != null) {
            product.setNmId(Long.valueOf(data.get("nmID").toString()));
        }
        if (data.get("name") != null) {
            product.setName(data.get("name").toString());
        }
        if (data.get("vendor") != null) {
            product.setVendor(data.get("vendor").toString());
        }
        if (data.get("vendor_code") != null) {
            product.setVendorCode(data.get("vendor_code").toString());
        } else if (data.get("vendorCode") != null) {
            product.setVendorCode(data.get("vendorCode").toString());
        }
        if (data.get("price") != null) {
            product.setPrice(new BigDecimal(data.get("price").toString()));
        } else if (data.get("priceU") != null) {
            // Цены у WB часто в копейках с суффиксом U
            product.setPrice(new BigDecimal(data.get("priceU").toString()).divide(new BigDecimal("100")));
        } else if (data.get("basicPriceU") != null) {
            product.setPrice(new BigDecimal(data.get("basicPriceU").toString()).divide(new BigDecimal("100")));
        }
        if (data.get("discount") != null) {
            product.setDiscount(Integer.valueOf(data.get("discount").toString()));
        }
        if (data.get("price_with_discount") != null) {
            product.setPriceWithDiscount(new BigDecimal(data.get("price_with_discount").toString()));
        } else if (data.get("priceWithDiscount") != null) {
            product.setPriceWithDiscount(new BigDecimal(data.get("priceWithDiscount").toString()));
        } else if (data.get("salePriceU") != null) {
            product.setPriceWithDiscount(new BigDecimal(data.get("salePriceU").toString()).divide(new BigDecimal("100")));
        }
        if (data.get("sale_price") != null) {
            product.setSalePrice(new BigDecimal(data.get("sale_price").toString()));
        } else if (data.get("salePrice") != null) {
            product.setSalePrice(new BigDecimal(data.get("salePrice").toString()));
        } else if (data.get("salePriceU") != null) {
            product.setSalePrice(new BigDecimal(data.get("salePriceU").toString()).divide(new BigDecimal("100")));
        }
        if (data.get("sale") != null) {
            product.setSale(Integer.valueOf(data.get("sale").toString()));
        }
        if (data.get("basic_sale") != null) {
            product.setBasicSale(Integer.valueOf(data.get("basic_sale").toString()));
        } else if (data.get("basicSale") != null) {
            product.setBasicSale(Integer.valueOf(data.get("basicSale").toString()));
        }
        if (data.get("basic_price_u") != null) {
            product.setBasicPriceU(new BigDecimal(data.get("basic_price_u").toString()));
        } else if (data.get("basicPriceU") != null) {
            product.setBasicPriceU(new BigDecimal(data.get("basicPriceU").toString()).divide(new BigDecimal("100")));
        }
        if (data.get("total_quantity") != null) {
            product.setTotalQuantity(Integer.valueOf(data.get("total_quantity").toString()));
        } else if (data.get("totalQuantity") != null) {
            product.setTotalQuantity(Integer.valueOf(data.get("totalQuantity").toString()));
        }
        if (data.get("quantity_not_in_orders") != null) {
            product.setQuantityNotInOrders(Integer.valueOf(data.get("quantity_not_in_orders").toString()));
        } else if (data.get("quantityNotInOrders") != null) {
            product.setQuantityNotInOrders(Integer.valueOf(data.get("quantityNotInOrders").toString()));
        }
        if (data.get("quantity_full") != null) {
            product.setQuantityFull(Integer.valueOf(data.get("quantity_full").toString()));
        } else if (data.get("quantityFull") != null) {
            product.setQuantityFull(Integer.valueOf(data.get("quantityFull").toString()));
        }
        if (data.get("in_way_to_client") != null) {
            product.setInWayToClient(Integer.valueOf(data.get("in_way_to_client").toString()));
        } else if (data.get("inWayToClient") != null) {
            product.setInWayToClient(Integer.valueOf(data.get("inWayToClient").toString()));
        }
        if (data.get("in_way_from_client") != null) {
            product.setInWayFromClient(Integer.valueOf(data.get("in_way_from_client").toString()));
        } else if (data.get("inWayFromClient") != null) {
            product.setInWayFromClient(Integer.valueOf(data.get("inWayFromClient").toString()));
        }
        if (data.get("category") != null) {
            product.setCategory(data.get("category").toString());
        }
        if (data.get("subject") != null) {
            product.setSubject(data.get("subject").toString());
        }
        if (data.get("brand") != null) {
            product.setBrand(data.get("brand").toString());
        }
        if (data.get("colors") != null) {
            product.setColors(data.get("colors").toString());
        }
        if (data.get("sizes") != null) {
            product.setSizes(data.get("sizes").toString());
            // Если цены вложены в sizes[], вытащим первую
            if ((product.getPrice() == null || product.getPrice().signum() == 0)
                    || product.getPriceWithDiscount() == null || product.getSalePrice() == null) {
                Object sizes = data.get("sizes");
                if (sizes instanceof List<?> list && !list.isEmpty() && list.get(0) instanceof Map) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> s0 = (Map<String, Object>) list.get(0);
                    if (product.getPrice() == null && s0.get("price") != null) {
                        product.setPrice(new BigDecimal(s0.get("price").toString()));
                    }
                    if (product.getPriceWithDiscount() == null && s0.get("discountedPrice") != null) {
                        product.setPriceWithDiscount(new BigDecimal(s0.get("discountedPrice").toString()));
                    }
                    if (product.getSalePrice() == null && s0.get("discountedPrice") != null) {
                        product.setSalePrice(new BigDecimal(s0.get("discountedPrice").toString()));
                    }
                }
            }
        }
        return product;
    }

    private void updateWbProductFromData(WbProduct existingProduct, Map<String, Object> data) {
        if (data.get("name") != null) {
            existingProduct.setName(data.get("name").toString());
        }
        if (data.get("price") != null) {
            existingProduct.setPrice(new BigDecimal(data.get("price").toString()));
        }
        if (data.get("price_with_discount") != null) {
            existingProduct.setPriceWithDiscount(new BigDecimal(data.get("price_with_discount").toString()));
        }
        if (data.get("sale_price") != null) {
            existingProduct.setSalePrice(new BigDecimal(data.get("sale_price").toString()));
        }
        if (data.get("discount") != null) {
            existingProduct.setDiscount(Integer.valueOf(data.get("discount").toString()));
        }
        if (data.get("total_quantity") != null) {
            existingProduct.setTotalQuantity(Integer.valueOf(data.get("total_quantity").toString()));
        }
        if (data.get("sizes") instanceof List<?> list && !list.isEmpty() && list.get(0) instanceof Map) {
            @SuppressWarnings("unchecked")
            Map<String, Object> s0 = (Map<String, Object>) list.get(0);
            if (existingProduct.getPrice() == null && s0.get("price") != null) {
                existingProduct.setPrice(new BigDecimal(s0.get("price").toString()));
            }
            if (existingProduct.getPriceWithDiscount() == null && s0.get("discountedPrice") != null) {
                existingProduct.setPriceWithDiscount(new BigDecimal(s0.get("discountedPrice").toString()));
            }
            if (existingProduct.getSalePrice() == null && s0.get("discountedPrice") != null) {
                existingProduct.setSalePrice(new BigDecimal(s0.get("discountedPrice").toString()));
            }
        }
        existingProduct.setUpdatedAt(java.time.LocalDateTime.now());
    }

    /**
     * Нормализует элемент Discounts/Prices: разворачивает sizes[0] и конвертирует *_U суммы из копеек в рубли.
     */
    private Map<String, Object> normalizeDiscountsPricesItem(Map<String, Object> item) {
        Map<String, Object> out = new LinkedHashMap<>(item);
        // nmID uniform
        if (!out.containsKey("nm_id") && out.get("nmID") != null) out.put("nm_id", out.get("nmID"));
        if (!out.containsKey("vendor_code") && out.get("vendorCode") != null) out.put("vendor_code", out.get("vendorCode"));

        // prices from *_U
        if (out.get("basicPriceU") != null && out.get("price") == null) {
            out.put("price", new BigDecimal(out.get("basicPriceU").toString()).divide(new BigDecimal("100")));
        }
        if (out.get("salePriceU") != null && out.get("sale_price") == null) {
            out.put("sale_price", new BigDecimal(out.get("salePriceU").toString()).divide(new BigDecimal("100")));
        }

        // from sizes[0]
        Object sizes = out.get("sizes");
        if (sizes instanceof List<?> list && !list.isEmpty() && list.get(0) instanceof Map) {
            @SuppressWarnings("unchecked")
            Map<String, Object> s0 = (Map<String, Object>) list.get(0);
            if (out.get("price") == null && s0.get("price") != null) {
                out.put("price", new BigDecimal(s0.get("price").toString()));
            }
            if (out.get("price_with_discount") == null && s0.get("discountedPrice") != null) {
                out.put("price_with_discount", new BigDecimal(s0.get("discountedPrice").toString()));
            }
            if (out.get("sale_price") == null && s0.get("discountedPrice") != null) {
                out.put("sale_price", new BigDecimal(s0.get("discountedPrice").toString()));
            }
        }
        return out;
    }
}
