package com.marketplacehelper.service;

import com.marketplacehelper.dto.ProductAnalyticsDto;
import com.marketplacehelper.model.Product;
import com.marketplacehelper.model.WbProduct;
import com.marketplacehelper.repository.ProductRepository;
import com.marketplacehelper.repository.WbProductRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class AnalyticsService {

    private final ProductRepository productRepository;
    private final WbProductRepository wbProductRepository;

    public AnalyticsService(ProductRepository productRepository, WbProductRepository wbProductRepository) {
        this.productRepository = productRepository;
        this.wbProductRepository = wbProductRepository;
    }

    public List<ProductAnalyticsDto> buildProductAnalytics(boolean includeWithoutWb) {
        List<Product> products = productRepository.findAll();
        List<WbProduct> wbProducts = wbProductRepository.findAll();

        Map<String, WbProduct> wbByArticle = wbProducts.stream()
                .filter(product -> product.getNmId() != null)
                .collect(Collectors.toMap(product -> String.valueOf(product.getNmId()), product -> product, (a, b) -> a));

        Map<String, WbProduct> wbByVendorCode = wbProducts.stream()
                .filter(product -> product.getVendorCode() != null && !product.getVendorCode().isBlank())
                .collect(Collectors.toMap(product -> product.getVendorCode().trim(), product -> product, (a, b) -> a));

        List<ProductAnalyticsDto> result = new ArrayList<>();
        for (Product product : products) {
            Optional<WbProduct> wbProduct = findMatchingWbProduct(product, wbByArticle, wbByVendorCode);
            if (wbProduct.isEmpty() && !includeWithoutWb) {
                continue;
            }
            result.add(toDto(product, wbProduct.orElse(null)));
        }

        wbProducts.stream()
                .filter(wb -> includeWithoutWb && products.stream().noneMatch(p -> matches(p, wb)))
                .forEach(wb -> result.add(toDto(null, wb)));

        return result;
    }

    private Optional<WbProduct> findMatchingWbProduct(Product product,
                                                      Map<String, WbProduct> wbByArticle,
                                                      Map<String, WbProduct> wbByVendorCode) {
        if (product == null) {
            return Optional.empty();
        }
        if (product.getWbArticle() != null && !product.getWbArticle().isBlank()) {
            WbProduct byArticle = wbByArticle.get(product.getWbArticle().trim());
            if (byArticle == null) {
                byArticle = wbByVendorCode.get(product.getWbArticle().trim());
            }
            if (byArticle == null) {
                byArticle = parseAsNumber(product.getWbArticle())
                        .map(number -> wbByArticle.get(String.valueOf(number)))
                        .orElse(null);
            }
            if (byArticle != null) {
                return Optional.of(byArticle);
            }
        }
        return Optional.empty();
    }

    private boolean matches(Product product, WbProduct wbProduct) {
        if (product.getWbArticle() == null) {
            return false;
        }
        String article = product.getWbArticle().trim();
        if (article.isEmpty()) {
            return false;
        }
        if (String.valueOf(Optional.ofNullable(wbProduct.getNmId()).orElse(-1L)).equals(article)) {
            return true;
        }
        return article.equalsIgnoreCase(Optional.ofNullable(wbProduct.getVendorCode()).orElse(""));
    }

    private Optional<Long> parseAsNumber(String value) {
        try {
            return Optional.of(Long.parseLong(value.trim()));
        } catch (NumberFormatException ex) {
            return Optional.empty();
        }
    }

    private ProductAnalyticsDto toDto(Product product, WbProduct wbProduct) {
        ProductAnalyticsDto dto = new ProductAnalyticsDto();
        if (product != null) {
            dto.setProductId(product.getId());
            dto.setName(product.getName());
            dto.setWbArticle(product.getWbArticle());
            dto.setBrand(product.getBrand());
            dto.setCategory(product.getCategory());
            dto.setLocalPrice(product.getPrice());
            dto.setPurchasePrice(product.getPurchasePrice());
            dto.setLogisticsCost(product.getLogisticsCost());
            dto.setMarketingCost(product.getMarketingCost());
            dto.setOtherExpenses(product.getOtherExpenses());
            dto.setLocalStock(product.getStockQuantity());
        }
        if (wbProduct != null) {
            dto.setWbProductId(wbProduct.getId());
            dto.setVendorCode(wbProduct.getVendorCode());
            dto.setBrand(Optional.ofNullable(dto.getBrand()).orElse(wbProduct.getBrand()));
            dto.setCategory(Optional.ofNullable(dto.getCategory()).orElse(wbProduct.getCategory()));
            dto.setWbPrice(wbProduct.getPrice());
            dto.setWbDiscountPrice(Optional.ofNullable(wbProduct.getPriceWithDiscount()).orElse(wbProduct.getSalePrice()));
            dto.setWbStock(wbProduct.getTotalQuantity());
            dto.setName(Optional.ofNullable(dto.getName()).orElse(wbProduct.getName()));
            dto.setWbArticle(Optional.ofNullable(dto.getWbArticle())
                    .orElseGet(() -> wbProduct.getNmId() != null ? String.valueOf(wbProduct.getNmId()) : wbProduct.getVendorCode()));
        }

        calculateMargins(dto);
        return dto;
    }

    private void calculateMargins(ProductAnalyticsDto dto) {
        BigDecimal salePrice = Optional.ofNullable(dto.getWbDiscountPrice())
                .orElse(Optional.ofNullable(dto.getWbPrice())
                        .orElse(dto.getLocalPrice()));
        if (salePrice == null) {
            return;
        }

        BigDecimal totalCosts = sum(dto.getPurchasePrice(), dto.getLogisticsCost(), dto.getMarketingCost(), dto.getOtherExpenses());
        if (totalCosts == null) {
            return;
        }

        BigDecimal margin = salePrice.subtract(totalCosts);
        dto.setMargin(margin.setScale(2, RoundingMode.HALF_UP));
        if (salePrice.compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal percent = margin.divide(salePrice, 4, RoundingMode.HALF_UP)
                    .multiply(BigDecimal.valueOf(100))
                    .setScale(2, RoundingMode.HALF_UP);
            dto.setMarginPercent(percent);
        }
    }

    private BigDecimal sum(BigDecimal... values) {
        BigDecimal result = BigDecimal.ZERO;
        boolean hasValue = false;
        for (BigDecimal value : values) {
            if (value != null) {
                result = result.add(value);
                hasValue = true;
            }
        }
        return hasValue ? result : null;
    }
}
