package com.marketplacehelper.service;

import com.marketplacehelper.dto.AnalyticsReportDto;
import com.marketplacehelper.dto.ProductAnalyticsDto;
import com.marketplacehelper.dto.ProductDataSource;
import com.marketplacehelper.model.Product;
import com.marketplacehelper.model.WbProduct;
import com.marketplacehelper.repository.ProductRepository;
import com.marketplacehelper.repository.WbProductRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class AnalyticsService {

    private final ProductRepository productRepository;
    private final WbProductRepository wbProductRepository;
    private final BigDecimal defaultMinMarginPercent;
    private final boolean filterNegativeMargin;

    public AnalyticsService(ProductRepository productRepository,
                            WbProductRepository wbProductRepository,
                            @Value("${app.analytics.min-margin-percent:0}") BigDecimal defaultMinMarginPercent,
                            @Value("${app.analytics.filter-negative-margin:true}") boolean filterNegativeMargin) {
        this.productRepository = productRepository;
        this.wbProductRepository = wbProductRepository;
        this.defaultMinMarginPercent = defaultMinMarginPercent != null ? defaultMinMarginPercent : BigDecimal.ZERO;
        this.filterNegativeMargin = filterNegativeMargin;
    }

    public AnalyticsReportDto buildProductAnalyticsReport(boolean includeWithoutWb,
                                                          BigDecimal requestedMinMarginPercent,
                                                          boolean includeUnprofitable) {
        List<Product> products = productRepository.findAll();
        List<WbProduct> wbProducts = wbProductRepository.findAll();

        Map<String, WbProduct> wbByArticle = wbProducts.stream()
                .filter(product -> product.getNmId() != null)
                .collect(Collectors.toMap(product -> String.valueOf(product.getNmId()), product -> product, (a, b) -> a));

        Map<String, WbProduct> wbByVendorCode = wbProducts.stream()
                .filter(product -> product.getVendorCode() != null && !product.getVendorCode().isBlank())
                .collect(Collectors.toMap(product -> product.getVendorCode().trim(), product -> product, (a, b) -> a));

        BigDecimal marginThreshold = requestedMinMarginPercent != null
                ? requestedMinMarginPercent
                : defaultMinMarginPercent;

        List<ProductAnalyticsDto> allItems = new ArrayList<>();
        for (Product product : products) {
            Optional<WbProduct> wbProduct = findMatchingWbProduct(product, wbByArticle, wbByVendorCode);
            if (wbProduct.isEmpty() && !includeWithoutWb) {
                continue;
            }
            allItems.add(toDto(product, wbProduct.orElse(null), marginThreshold));
        }

        wbProducts.stream()
                .filter(wb -> includeWithoutWb && products.stream().noneMatch(p -> matches(p, wb)))
                .forEach(wb -> allItems.add(toDto(null, wb, marginThreshold)));

        sortAnalytics(allItems);

        List<ProductAnalyticsDto> profitable = allItems.stream()
                .filter(ProductAnalyticsDto::isProfitable)
                .collect(Collectors.toList());

        List<ProductAnalyticsDto> requiresAttention = allItems.stream()
                .filter(ProductAnalyticsDto::isRequiresCorrection)
                .collect(Collectors.toList());

        AnalyticsReportDto reportDto = new AnalyticsReportDto();
        reportDto.setAppliedMinMarginPercent(marginThreshold);
        reportDto.setProfitable(profitable);
        reportDto.setRequiresAttention(requiresAttention);
        reportDto.setProfitableCount(profitable.size());
        reportDto.setRequiresAttentionCount(requiresAttention.size());
        reportDto.setTotalProducts(allItems.size());
        reportDto.setAllItems(includeUnprofitable ? new ArrayList<>(allItems) : new ArrayList<>(profitable));
        return reportDto;
    }

    private Optional<WbProduct> findMatchingWbProduct(Product product,
                                                      Map<String, WbProduct> wbByArticle,
                                                      Map<String, WbProduct> wbByVendorCode) {
        if (product == null) {
            return Optional.empty();
        }
        Optional<WbProduct> directMatch = matchByKey(product.getWbArticle(), wbByArticle, wbByVendorCode);
        if (directMatch.isPresent()) {
            return directMatch;
        }
        Optional<WbProduct> supplierMatch = matchByKey(product.getSupplierArticle(), wbByArticle, wbByVendorCode);
        return supplierMatch;
    }

    private Optional<WbProduct> matchByKey(String key,
                                           Map<String, WbProduct> wbByArticle,
                                           Map<String, WbProduct> wbByVendorCode) {
        if (key == null || key.isBlank()) {
            return Optional.empty();
        }
        String trimmed = key.trim();
        WbProduct byArticle = wbByArticle.get(trimmed);
        if (byArticle != null) {
            return Optional.of(byArticle);
        }
        WbProduct byVendor = wbByVendorCode.get(trimmed);
        if (byVendor != null) {
            return Optional.of(byVendor);
        }
        return parseAsNumber(trimmed)
                .map(number -> wbByArticle.get(String.valueOf(number)))
                .flatMap(value -> Optional.ofNullable(value));
    }

    private boolean matches(Product product, WbProduct wbProduct) {
        String articleKey = product.getWbArticle();
        if (articleKey == null || articleKey.trim().isEmpty()) {
            articleKey = product.getSupplierArticle();
        }
        if (articleKey == null) {
            return false;
        }
        String article = articleKey.trim();
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

    private ProductAnalyticsDto toDto(Product product, WbProduct wbProduct, BigDecimal marginThreshold) {
        ProductAnalyticsDto dto = new ProductAnalyticsDto();
        if (product != null && wbProduct != null) {
            dto.setDataSource(ProductDataSource.MERGED);
        } else if (product != null) {
            dto.setDataSource(ProductDataSource.LOCAL_ONLY);
        } else {
            dto.setDataSource(ProductDataSource.WB_ONLY);
        }

        if (product != null) {
            dto.setProductId(product.getId());
            dto.setName(product.getName());
            dto.setWbArticle(product.getWbArticle());
            dto.setSupplierArticle(product.getSupplierArticle());
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
        applyValidation(dto, marginThreshold);
        return dto;
    }

    private void sortAnalytics(List<ProductAnalyticsDto> items) {
        Comparator<ProductAnalyticsDto> byProfitable = Comparator
                .comparing(ProductAnalyticsDto::isProfitable)
                .reversed();
        Comparator<ProductAnalyticsDto> byMargin = Comparator
                .comparing(ProductAnalyticsDto::getMargin, Comparator.nullsLast(Comparator.naturalOrder()))
                .reversed();
        Comparator<ProductAnalyticsDto> byName = Comparator
                .comparing(dto -> Optional.ofNullable(dto.getName()).orElse(""), String.CASE_INSENSITIVE_ORDER);

        items.sort(byProfitable
                .thenComparing(ProductAnalyticsDto::isRequiresCorrection)
                .thenComparing(byMargin)
                .thenComparing(byName));
    }

    private void applyValidation(ProductAnalyticsDto dto, BigDecimal marginThreshold) {
        if (dto.getDataSource() == ProductDataSource.LOCAL_ONLY) {
            dto.addWarning("Загружено из Excel — нет данных из кабинета WB.");
        } else if (dto.getDataSource() == ProductDataSource.WB_ONLY) {
            dto.addWarning("Нет корпоративных данных — загрузите Excel для сопоставления.");
        }

        if (dto.getPurchasePrice() == null) {
            dto.addWarning("Не заполнено поле «Закупка».");
        }
        if (dto.getLogisticsCost() == null) {
            dto.addWarning("Не указана логистика.");
        }
        if (dto.getMarketingCost() == null) {
            dto.addWarning("Не указаны маркетинговые расходы.");
        }
        if (dto.getOtherExpenses() == null) {
            dto.addWarning("Не указаны прочие расходы.");
        }
        if (dto.getWbPrice() != null && dto.getWbDiscountPrice() != null
                && dto.getWbDiscountPrice().compareTo(dto.getWbPrice()) > 0) {
            dto.addWarning("Цена со скидкой превышает базовую цену WB.");
        }

        if (dto.getMargin() == null) {
            dto.addWarning("Не удалось рассчитать маржу.");
        } else {
            if (dto.getMargin().compareTo(BigDecimal.ZERO) < 0) {
                dto.setNegativeMargin(true);
                if (filterNegativeMargin) {
                    dto.addWarning("Маржа отрицательная. Проверьте расходы и цены.");
                }
            }
            if (dto.getMarginPercent() == null || dto.getMarginPercent().compareTo(marginThreshold) < 0) {
                dto.setMarginBelowThreshold(true);
                dto.addWarning(String.format(Locale.ROOT,
                        "Маржа ниже порога %s%%.",
                        marginThreshold.stripTrailingZeros().toPlainString()));
            }
        }

        boolean hasWarnings = !dto.getWarnings().isEmpty();
        boolean profitable = dto.getMargin() != null
                && dto.getMarginPercent() != null
                && dto.getMargin().compareTo(BigDecimal.ZERO) >= 0
                && dto.getMarginPercent().compareTo(marginThreshold) >= 0
                && !hasWarnings;
        dto.setProfitable(profitable);
        dto.setRequiresCorrection(hasWarnings);
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

    public byte[] exportProfitableProductsAsCsv(boolean includeWithoutWb,
                                                BigDecimal requestedMinMarginPercent) {
        AnalyticsReportDto report = buildProductAnalyticsReport(includeWithoutWb, requestedMinMarginPercent, false);
        StringBuilder builder = new StringBuilder();
        builder.append("Артикул;Название;Источник;Маржа;Маржа %;Цена WB;Цена со скидкой;Закупка;Логистика;Маркетинг;Прочие;Остаток лок.;Остаток WB\n");
        for (ProductAnalyticsDto dto : report.getProfitable()) {
            builder.append(csv(dto.getWbArticle()))
                    .append(';').append(csv(dto.getName()))
                    .append(';').append(csv(sourceLabel(dto.getDataSource())))
                    .append(';').append(decimal(dto.getMargin()))
                    .append(';').append(decimal(dto.getMarginPercent()))
                    .append(';').append(decimal(dto.getWbPrice()))
                    .append(';').append(decimal(dto.getWbDiscountPrice()))
                    .append(';').append(decimal(dto.getPurchasePrice()))
                    .append(';').append(decimal(dto.getLogisticsCost()))
                    .append(';').append(decimal(dto.getMarketingCost()))
                    .append(';').append(decimal(dto.getOtherExpenses()))
                    .append(';').append(integer(dto.getLocalStock()))
                    .append(';').append(integer(dto.getWbStock()))
                    .append('\n');
        }
        return builder.toString().getBytes(StandardCharsets.UTF_8);
    }

    private String csv(String value) {
        if (value == null) {
            return "";
        }
        boolean needsQuotes = value.contains(";") || value.contains("\"") || value.contains("\n");
        String escaped = value.replace("\"", "\"\"");
        return needsQuotes ? "\"" + escaped + "\"" : escaped;
    }

    private String decimal(BigDecimal value) {
        return value != null ? value.setScale(2, RoundingMode.HALF_UP).toPlainString() : "";
    }

    private String integer(Integer value) {
        return value != null ? value.toString() : "";
    }

    private String sourceLabel(ProductDataSource dataSource) {
        if (dataSource == null) {
            return "";
        }
        return switch (dataSource) {
            case MERGED -> "WB + Excel";
            case LOCAL_ONLY -> "Excel";
            case WB_ONLY -> "Wildberries";
        };
    }
}
