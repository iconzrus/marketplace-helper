package com.marketplacehelper.service;

import com.marketplacehelper.config.WbApiConfig;
import com.marketplacehelper.model.WbProduct;
import com.marketplacehelper.repository.WbProductRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

@Service
public class WbApiService {
    
    private final RestTemplate wbRestTemplate;
    private final WbApiConfig wbApiConfig;
    private final WbProductRepository wbProductRepository;
    
    @Autowired
    public WbApiService(RestTemplate wbRestTemplate, WbApiConfig wbApiConfig, WbProductRepository wbProductRepository) {
        this.wbRestTemplate = wbRestTemplate;
        this.wbApiConfig = wbApiConfig;
        this.wbProductRepository = wbProductRepository;
    }
    
    /**
     * Получение товаров с ценами из WB API
     * Эндпоинт: GET /api/v2/list/goods/filter
     */
    public List<Map<String, Object>> getGoodsWithPrices() {
        try {
            String url = wbApiConfig.getWbApiBaseUrl() + "/api/v2/list/goods/filter";
            
            ResponseEntity<List<Map<String, Object>>> response = wbRestTemplate.exchange(
                url,
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<List<Map<String, Object>>>() {}
            );
            
            return response.getBody();
        } catch (Exception e) {
            throw new RuntimeException("Ошибка при получении товаров из WB API: " + e.getMessage(), e);
        }
    }
    
    /**
     * Получение товаров с ценами с фильтрацией
     */
    public List<Map<String, Object>> getGoodsWithPricesFiltered(Map<String, String> filters) {
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
            
            String url = urlBuilder.toString();
            
            ResponseEntity<List<Map<String, Object>>> response = wbRestTemplate.exchange(
                url,
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<List<Map<String, Object>>>() {}
            );
            
            return response.getBody();
        } catch (Exception e) {
            throw new RuntimeException("Ошибка при получении товаров из WB API с фильтрацией: " + e.getMessage(), e);
        }
    }
    
    /**
     * Проверка подключения к WB API
     * Эндпоинт: GET /ping
     */
    public Map<String, Object> pingWbApi() {
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
    
    /**
     * Получение информации о продавце
     * Эндпоинт: GET /api/v1/seller-info
     */
    public Map<String, Object> getSellerInfo() {
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
    
    /**
     * Синхронизация товаров из WB API в локальную базу данных
     */
    public void syncProductsFromWbApi() {
        try {
            List<Map<String, Object>> wbProducts = getGoodsWithPrices();
            
            for (Map<String, Object> productData : wbProducts) {
                WbProduct product = convertToWbProduct(productData);
                
                // Проверяем, существует ли товар уже в базе
                wbProductRepository.findByNmId(product.getNmId())
                    .ifPresentOrElse(
                        existingProduct -> {
                            // Обновляем существующий товар
                            updateWbProductFromData(existingProduct, productData);
                            wbProductRepository.save(existingProduct);
                        },
                        () -> {
                            // Создаем новый товар
                            wbProductRepository.save(product);
                        }
                    );
            }
        } catch (Exception e) {
            throw new RuntimeException("Ошибка при синхронизации товаров: " + e.getMessage(), e);
        }
    }
    
    /**
     * Конвертация данных из WB API в модель WbProduct
     */
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
            product.setPrice(new java.math.BigDecimal(data.get("price").toString()));
        }
        if (data.get("discount") != null) {
            product.setDiscount(Integer.valueOf(data.get("discount").toString()));
        }
        if (data.get("price_with_discount") != null) {
            product.setPriceWithDiscount(new java.math.BigDecimal(data.get("price_with_discount").toString()));
        }
        if (data.get("sale_price") != null) {
            product.setSalePrice(new java.math.BigDecimal(data.get("sale_price").toString()));
        }
        if (data.get("sale") != null) {
            product.setSale(Integer.valueOf(data.get("sale").toString()));
        }
        if (data.get("basic_sale") != null) {
            product.setBasicSale(Integer.valueOf(data.get("basic_sale").toString()));
        }
        if (data.get("basic_price_u") != null) {
            product.setBasicPriceU(new java.math.BigDecimal(data.get("basic_price_u").toString()));
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
    
    /**
     * Обновление существующего товара данными из WB API
     */
    private void updateWbProductFromData(WbProduct existingProduct, Map<String, Object> data) {
        if (data.get("name") != null) {
            existingProduct.setName(data.get("name").toString());
        }
        if (data.get("price") != null) {
            existingProduct.setPrice(new java.math.BigDecimal(data.get("price").toString()));
        }
        if (data.get("discount") != null) {
            existingProduct.setDiscount(Integer.valueOf(data.get("discount").toString()));
        }
        if (data.get("total_quantity") != null) {
            existingProduct.setTotalQuantity(Integer.valueOf(data.get("total_quantity").toString()));
        }
        // Обновляем timestamp
        existingProduct.setUpdatedAt(java.time.LocalDateTime.now());
    }
}



