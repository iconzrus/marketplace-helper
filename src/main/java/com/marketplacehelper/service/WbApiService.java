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
            if (data instanceof List<?> list) {
                @SuppressWarnings("unchecked")
                List<Map<String, Object>> casted = (List<Map<String, Object>>) (List<?>) list;
                return casted;
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
            StringBuilder urlBuilder = new StringBuilder(wbApiConfig.getWbApiBaseUrl() + "/api/v2/list/goods/filter");
            if (filters != null && !filters.isEmpty()) {
                urlBuilder.append("?");
                boolean first = true;
                for (Map.Entry<String, String> entry : filters.entrySet()) {
                    if (!first) {
                        urlBuilder.append("&");
                    }
                    urlBuilder.append(entry.getKey()).append("=").append(entry.getValue());
                    first = false;
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
            if (data instanceof List<?> list) {
                @SuppressWarnings("unchecked")
                List<Map<String, Object>> casted = (List<Map<String, Object>>) (List<?>) list;
                return casted;
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
            Map<String, Object> response = new LinkedHashMap<>();
            response.put("company", "Demo Seller LLC");
            response.put("inn", "0000000000");
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

    private boolean shouldUseMock() {
        String token = wbApiConfig.getWbApiToken();
        return mockMode || token == null || token.isBlank();
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
        }
        if (data.get("name") != null) {
            product.setName(data.get("name").toString());
        }
        if (data.get("vendor") != null) {
            product.setVendor(data.get("vendor").toString());
        }
        if (data.get("vendor_code") != null) {
            product.setVendorCode(data.get("vendor_code").toString());
        }
        if (data.get("price") != null) {
            product.setPrice(new BigDecimal(data.get("price").toString()));
        }
        if (data.get("discount") != null) {
            product.setDiscount(Integer.valueOf(data.get("discount").toString()));
        }
        if (data.get("price_with_discount") != null) {
            product.setPriceWithDiscount(new BigDecimal(data.get("price_with_discount").toString()));
        }
        if (data.get("sale_price") != null) {
            product.setSalePrice(new BigDecimal(data.get("sale_price").toString()));
        }
        if (data.get("sale") != null) {
            product.setSale(Integer.valueOf(data.get("sale").toString()));
        }
        if (data.get("basic_sale") != null) {
            product.setBasicSale(Integer.valueOf(data.get("basic_sale").toString()));
        }
        if (data.get("basic_price_u") != null) {
            product.setBasicPriceU(new BigDecimal(data.get("basic_price_u").toString()));
        }
        if (data.get("total_quantity") != null) {
            product.setTotalQuantity(Integer.valueOf(data.get("total_quantity").toString()));
        }
        if (data.get("quantity_not_in_orders") != null) {
            product.setQuantityNotInOrders(Integer.valueOf(data.get("quantity_not_in_orders").toString()));
        }
        if (data.get("quantity_full") != null) {
            product.setQuantityFull(Integer.valueOf(data.get("quantity_full").toString()));
        }
        if (data.get("in_way_to_client") != null) {
            product.setInWayToClient(Integer.valueOf(data.get("in_way_to_client").toString()));
        }
        if (data.get("in_way_from_client") != null) {
            product.setInWayFromClient(Integer.valueOf(data.get("in_way_from_client").toString()));
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
        existingProduct.setUpdatedAt(java.time.LocalDateTime.now());
    }
}
