package com.marketplacehelper.service;

import com.marketplacehelper.dto.AutoFillRequestDto;
import com.marketplacehelper.dto.AutoFillResultDto;
import com.marketplacehelper.model.Product;
import com.marketplacehelper.model.WbProduct;
import com.marketplacehelper.repository.ProductRepository;
import com.marketplacehelper.repository.WbProductRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

@Service
public class DemoDataService {

    private final ProductRepository productRepository;
    private final WbProductRepository wbProductRepository;

    public DemoDataService(ProductRepository productRepository, WbProductRepository wbProductRepository) {
        this.productRepository = productRepository;
        this.wbProductRepository = wbProductRepository;
    }

    @Transactional
    public AutoFillResultDto autoFillMissingCosts(AutoFillRequestDto request) {
        List<Product> products = productRepository.findAll();
        int affected = 0;
        AutoFillResultDto result = new AutoFillResultDto();
        int limit = request.getLimit() != null && request.getLimit() > 0 ? request.getLimit() : Integer.MAX_VALUE;

        for (Product product : products) {
            List<String> updatedFields = new ArrayList<>();

            BigDecimal basePrice = product.getPrice();
            if (basePrice == null) {
                continue;
            }

            // purchase
            if ((!request.isOnlyIfMissing() || product.getPurchasePrice() == null) && request.getPurchasePercentOfPrice() != null) {
                BigDecimal purchase = percentageOf(basePrice, request.getPurchasePercentOfPrice());
                product.setPurchasePrice(purchase);
                updatedFields.add("purchasePrice");
            }

            // logistics
            if ((!request.isOnlyIfMissing() || product.getLogisticsCost() == null) && request.getLogisticsFixed() != null) {
                product.setLogisticsCost(request.getLogisticsFixed());
                updatedFields.add("logisticsCost");
            }

            // marketing
            if ((!request.isOnlyIfMissing() || product.getMarketingCost() == null) && request.getMarketingPercentOfPrice() != null) {
                BigDecimal marketing = percentageOf(basePrice, request.getMarketingPercentOfPrice());
                product.setMarketingCost(marketing);
                updatedFields.add("marketingCost");
            }

            // other
            if ((!request.isOnlyIfMissing() || product.getOtherExpenses() == null) && request.getOtherFixed() != null) {
                product.setOtherExpenses(request.getOtherFixed());
                updatedFields.add("otherExpenses");
            }

            if (!updatedFields.isEmpty()) {
                productRepository.save(product);
                affected++;
                AutoFillResultDto.AffectedItem item = new AutoFillResultDto.AffectedItem();
                item.setProductId(product.getId());
                item.setName(product.getName());
                item.setWbArticle(product.getWbArticle());
                item.setUpdatedFields(updatedFields);
                result.addItem(item);
                if (affected >= limit) break;
            }
        }

        result.setAffectedCount(affected);
        return result;
    }

    private BigDecimal percentageOf(BigDecimal base, BigDecimal percent) {
        return base.multiply(percent).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
    }

    @Transactional
    public int fillRandomAll() {
        int affected = 0;

        // Ensure each WB card has a local Product and fill random costs
        List<WbProduct> wbProducts = wbProductRepository.findAll();
        for (WbProduct wb : wbProducts) {
            String articleKey = wb.getNmId() != null ? String.valueOf(wb.getNmId()) : normalizeString(wb.getVendorCode());
            Product product = null;
            if (articleKey != null && !articleKey.isBlank()) {
                product = productRepository.findByWbArticle(articleKey).orElse(null);
            }
            if (product == null) {
                product = new Product();
                product.setName(wb.getName());
                product.setBrand(wb.getBrand());
                product.setCategory(wb.getCategory());
                product.setWbArticle(articleKey);
                product.setPrice(firstNonNull4(wb.getPriceWithDiscount(), wb.getSalePrice(), wb.getPrice(), randomPrice()));
            }
            // Overwrite costs with fresh random values every call
            overwriteAllCostsRandom(product);
            productRepository.save(product);
            affected++;
        }

        // Ensure each local Product has a WB card to be merged
        List<Product> products = productRepository.findAll();
        for (Product p : products) {
            String article = normalizeString(p.getWbArticle());
            WbProduct wb = null;
            if (article != null && !article.isBlank()) {
                // try nmId numeric match
                Long nm = parseLongSafely(article);
                if (nm != null) wb = wbProductRepository.findByNmId(nm).orElse(null);
                if (wb == null) wb = wbProductRepository.findByVendorCode(article).orElse(null);
            }
            if (wb == null) {
                wb = new WbProduct();
                Long nm = parseLongSafely(article);
                if (nm != null) {
                    wb.setNmId(nm);
                } else {
                    wb.setVendorCode(article != null && !article.isBlank() ? article : ("EXC-" + p.getId()));
                }
                wb.setName(p.getName());
                wb.setBrand(p.getBrand());
                wb.setCategory(p.getCategory());
            }
            // Always (re)fill WB fields randomly
            fillWbRandom(wb);
            wbProductRepository.save(wb);
            affected++;

            // ensure product has article to match WB
            if (p.getWbArticle() == null || p.getWbArticle().isBlank()) {
                if (wb.getNmId() != null) {
                    p.setWbArticle(String.valueOf(wb.getNmId()));
                } else if (wb.getVendorCode() != null) {
                    p.setWbArticle(wb.getVendorCode());
                }
            }
            // Overwrite costs with fresh random values every call
            overwriteAllCostsRandom(p);
            productRepository.save(p);
            if (!p.getWbArticle().isBlank()) {
                affected++;
            }
        }

        return affected;
    }

    private boolean overwriteAllCostsRandom(Product product) {
        boolean changed = true;
        BigDecimal price = product.getPrice();
        if (price == null) {
            price = randomPrice();
            product.setPrice(price);
        }
        product.setPurchasePrice(randPercent(price, 45, 65));
        product.setLogisticsCost(randFixed(60, 150));
        product.setMarketingCost(randPercent(price, 5, 15));
        product.setOtherExpenses(randFixed(0, 120));
        return changed;
    }

    private boolean fillWbRandom(WbProduct wb) {
        boolean changed = false;
        BigDecimal base = firstNonNull4(wb.getPriceWithDiscount(), wb.getSalePrice(), wb.getPrice(), null);
        if (base == null) {
            base = randomPrice();
            wb.setPrice(base);
            changed = true;
        }
        if (wb.getDiscount() == null) {
            int d = (int)(Math.random() * 30);
            wb.setDiscount(d);
            changed = true;
        }
        if (wb.getPriceWithDiscount() == null) {
            BigDecimal p = firstNonNull2(wb.getPrice(), base);
            BigDecimal disc = p.multiply(BigDecimal.valueOf((100 - (wb.getDiscount() != null ? wb.getDiscount() : 10)))).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
            wb.setPriceWithDiscount(disc);
            changed = true;
        }
        if (wb.getSalePrice() == null) {
            wb.setSalePrice(firstNonNull2(wb.getPrice(), base));
            changed = true;
        }
        if (wb.getTotalQuantity() == null) {
            wb.setTotalQuantity((int)(Math.random() * 200));
            changed = true;
        }
        return changed;
    }

    private BigDecimal firstNonNull2(BigDecimal a, BigDecimal b) {
        return a != null ? a : b;
    }

    private BigDecimal firstNonNull4(BigDecimal a, BigDecimal b, BigDecimal c, BigDecimal d) {
        if (a != null) return a;
        if (b != null) return b;
        if (c != null) return c;
        return d;
    }

    private BigDecimal randomPrice() {
        int v = 300 + (int)(Math.random() * 4700);
        return BigDecimal.valueOf(v);
    }

    private BigDecimal randFixed(int min, int max) {
        int v = min + (int)(Math.random() * Math.max(1, (max - min + 1)));
        return BigDecimal.valueOf(v);
    }

    private BigDecimal randPercent(BigDecimal base, int minPct, int maxPct) {
        int pct = minPct + (int)(Math.random() * Math.max(1, (maxPct - minPct + 1)));
        return percentageOf(base, BigDecimal.valueOf(pct));
    }

    private String normalizeString(String v) {
        if (v == null) return null;
        return v.trim();
    }

    private Long parseLongSafely(String v) {
        if (v == null) return null;
        try {
            return Long.parseLong(v.replaceAll("\\s+", ""));
        } catch (NumberFormatException ex) {
            return null;
        }
    }
}


