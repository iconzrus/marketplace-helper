package com.marketplacehelper.config;

import com.marketplacehelper.model.Product;
import com.marketplacehelper.repository.ProductRepository;
import com.marketplacehelper.service.WbApiService;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;

@Component
public class DemoDataInitializer {

    private final ProductRepository productRepository;
    private final WbApiService wbApiService;
    private final boolean demoEnabled;

    public DemoDataInitializer(ProductRepository productRepository,
                               WbApiService wbApiService,
                               @Value("${app.demo-data.enabled:true}") boolean demoEnabled) {
        this.productRepository = productRepository;
        this.wbApiService = wbApiService;
        this.demoEnabled = demoEnabled;
    }

    @PostConstruct
    public void init() {
        if (!demoEnabled) {
            return;
        }
        if (productRepository.count() == 0) {
            seedProducts();
        }
        if (wbApiService != null) {
            wbApiService.syncProductsFromWbApi();
        }
    }

    private void seedProducts() {
        Product smartphone = new Product();
        smartphone.setName("Смартфон Helios X200");
        smartphone.setWbArticle("100001");
        smartphone.setPrice(new BigDecimal("15990"));
        smartphone.setPurchasePrice(new BigDecimal("11200"));
        smartphone.setLogisticsCost(new BigDecimal("1200"));
        smartphone.setMarketingCost(new BigDecimal("800"));
        smartphone.setOtherExpenses(new BigDecimal("500"));
        smartphone.setStockQuantity(60);
        smartphone.setCategory("Электроника");
        smartphone.setBrand("Helios");

        Product headphones = new Product();
        headphones.setName("Беспроводные наушники WavePods");
        headphones.setWbArticle("WAVE-AIR");
        headphones.setPrice(new BigDecimal("5490"));
        headphones.setPurchasePrice(new BigDecimal("3100"));
        headphones.setLogisticsCost(new BigDecimal("450"));
        headphones.setMarketingCost(new BigDecimal("350"));
        headphones.setOtherExpenses(new BigDecimal("200"));
        headphones.setStockQuantity(120);
        headphones.setCategory("Электроника");
        headphones.setBrand("Wave");

        Product backpack = new Product();
        backpack.setName("Рюкзак Explorer 35L");
        backpack.setWbArticle("100003");
        backpack.setPrice(new BigDecimal("3990"));
        backpack.setPurchasePrice(new BigDecimal("2100"));
        backpack.setLogisticsCost(new BigDecimal("380"));
        backpack.setMarketingCost(new BigDecimal("250"));
        backpack.setOtherExpenses(new BigDecimal("180"));
        backpack.setStockQuantity(30);
        backpack.setCategory("Спорт");
        backpack.setBrand("NordHike");

        Product smartwatch = new Product();
        smartwatch.setName("Умные часы PulseGo");
        smartwatch.setWbArticle("100004");
        smartwatch.setPrice(new BigDecimal("12990"));
        smartwatch.setPurchasePrice(new BigDecimal("8400"));
        smartwatch.setLogisticsCost(new BigDecimal("950"));
        smartwatch.setMarketingCost(new BigDecimal("620"));
        smartwatch.setOtherExpenses(new BigDecimal("350"));
        smartwatch.setStockQuantity(80);
        smartwatch.setCategory("Электроника");
        smartwatch.setBrand("Pulse");

        Product blender = new Product();
        blender.setName("Погружной блендер KitchenPro 900");
        blender.setWbArticle("KP-900");
        blender.setPrice(new BigDecimal("4990"));
        blender.setPurchasePrice(new BigDecimal("2900"));
        blender.setLogisticsCost(new BigDecimal("420"));
        blender.setMarketingCost(new BigDecimal("280"));
        blender.setOtherExpenses(new BigDecimal("190"));
        blender.setStockQuantity(55);
        blender.setCategory("Бытовая техника");
        blender.setBrand("KitchenPro");

        Product sneakers = new Product();
        sneakers.setName("Кроссовки Sprint Pro");
        sneakers.setWbArticle("100007");
        sneakers.setPrice(new BigDecimal("6290"));
        sneakers.setPurchasePrice(new BigDecimal("3600"));
        sneakers.setLogisticsCost(new BigDecimal("410"));
        sneakers.setMarketingCost(new BigDecimal("320"));
        sneakers.setOtherExpenses(new BigDecimal("260"));
        sneakers.setStockQuantity(90);
        sneakers.setCategory("Спорт");
        sneakers.setBrand("FastRun");

        productRepository.saveAll(List.of(smartphone, headphones, backpack, smartwatch, blender, sneakers));
    }
}
