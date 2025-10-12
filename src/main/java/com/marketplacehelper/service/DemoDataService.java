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
import java.util.Random;

@Service
public class DemoDataService {

    private final ProductRepository productRepository;
    private final WbProductRepository wbProductRepository;
    private final WbApiService wbApiService;

    public DemoDataService(ProductRepository productRepository, WbProductRepository wbProductRepository, WbApiService wbApiService) {
        this.productRepository = productRepository;
        this.wbProductRepository = wbProductRepository;
        this.wbApiService = wbApiService;
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
        // Generate a fresh mock "cabinet" of WB products (100-300)
        wbProductRepository.deleteAll();
        wbApiService.regenerateMockSeller(); // Generate new seller info
        int total = 100 + new java.util.Random().nextInt(201);
        List<WbProduct> batch = new java.util.ArrayList<>(total);
        for (int i = 0; i < total; i++) {
            WbProduct wb = new WbProduct();
            wb.setNmId(randomNmId());
            wb.setVendorCode("VN-" + wb.getNmId());
            wb.setName(randomName());
            wb.setBrand(randomBrand());
            wb.setCategory(randomCategory());
            fillWbRandom(wb);
            batch.add(wb);
        }
        wbProductRepository.saveAll(batch);
        return total;
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

    @Transactional
    public int generateDemo(int count, String type) {
        int total = Math.max(0, count);
        String t = type == null ? "both" : type.trim().toLowerCase();
        int created = 0;
        int excelCount = total;
        int wbCount = total;
        if ("excel".equals(t)) {
            wbCount = 0;
        } else if ("wb".equals(t)) {
            excelCount = 0;
        }
        if ("both".equals(t)) {
            excelCount = total / 2 + (total % 2);
            wbCount = total / 2;
        }

        for (int i = 0; i < excelCount; i++) {
            Product p = new Product();
            p.setName(randomName());
            p.setBrand(randomBrand());
            p.setCategory(randomCategory());
            p.setPrice(randomPrice());
            // оставляем пустые расходы, чтобы показать работу автозаполнения/пересчёта
            productRepository.save(p);
            created++;
        }

        for (int i = 0; i < wbCount; i++) {
            WbProduct wb = new WbProduct();
            wb.setNmId(randomNmId());
            wb.setVendorCode("VN-" + wb.getNmId());
            wb.setName(randomName());
            wb.setBrand(randomBrand());
            wb.setCategory(randomCategory());
            fillWbRandom(wb);
            wbProductRepository.save(wb);
            created++;
        }

        return created;
    }

    private final Random rnd = new Random();

    private String randomName() {
        String[] nouns = {"Чехол", "Рюкзак", "Коврик", "Перчатки", "Бутылка", "Лампа", "Подставка", "Мяч"};
        String[] adj = {"спортивный", "походный", "универсальный", "компактный", "профессиональный"};
        return nouns[rnd.nextInt(nouns.length)] + " " + adj[rnd.nextInt(adj.length)] + " #" + (100 + rnd.nextInt(900));
    }

    private String randomBrand() {
        String[] brands = {"FISHING BAND", "OutdoorPro", "UrbanGear", "LiteMax", "ProFit"};
        return brands[rnd.nextInt(brands.length)];
    }

    private String randomCategory() {
        String[] cats = {"Аксессуары", "Спорт", "Туризм", "Дом", "Электроника"};
        return cats[rnd.nextInt(cats.length)];
    }

    private Long randomNmId() {
        return 100000000L + Math.abs(rnd.nextLong() % 900000000L);
    }

    @Transactional
    public com.marketplacehelper.dto.DeleteResultDto deleteRandom(int count, boolean deleteAll) {
        com.marketplacehelper.dto.DeleteResultDto result = new com.marketplacehelper.dto.DeleteResultDto();
        if (deleteAll) {
            int prod = (int) productRepository.count();
            int wb = (int) wbProductRepository.count();
            productRepository.deleteAll();
            wbProductRepository.deleteAll();
            result.setDeletedProducts(prod);
            result.setDeletedWbProducts(wb);
            return result;
        }

        List<Product> products = productRepository.findAll();
        List<WbProduct> wbProducts = wbProductRepository.findAll();
        int toDeleteP = Math.min(count, products.size());
        int toDeleteW = Math.min(count, wbProducts.size());

        java.util.Collections.shuffle(products, rnd);
        java.util.Collections.shuffle(wbProducts, rnd);

        if (toDeleteP > 0) {
            productRepository.deleteAll(products.subList(0, toDeleteP));
        }
        if (toDeleteW > 0) {
            wbProductRepository.deleteAll(wbProducts.subList(0, toDeleteW));
        }
        result.setDeletedProducts(toDeleteP);
        result.setDeletedWbProducts(toDeleteW);
        return result;
    }
}


