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
        Product landingNetCaseBlack = new Product();
        landingNetCaseBlack.setName("Чехол для рыболовного подсака 80*85 см чёрный");
        landingNetCaseBlack.setWbArticle("ch5_bl");
        landingNetCaseBlack.setWbBarcode("4673756010669");
        landingNetCaseBlack.setPrice(new BigDecimal("2059.55"));
        landingNetCaseBlack.setPurchasePrice(new BigDecimal("1180.00"));
        landingNetCaseBlack.setLogisticsCost(new BigDecimal("76.00"));
        landingNetCaseBlack.setMarketingCost(new BigDecimal("120.00"));
        landingNetCaseBlack.setOtherExpenses(new BigDecimal("45.00"));
        landingNetCaseBlack.setStockQuantity(48);
        landingNetCaseBlack.setCategory("Другие принадлежности для рыбалки");
        landingNetCaseBlack.setBrand("FISHING BAND");

        Product landingNetCaseKhaki = new Product();
        landingNetCaseKhaki.setName("Чехол для подсака и подсачека непромокаемый 80*85 см хаки");
        landingNetCaseKhaki.setWbArticle("ch1");
        landingNetCaseKhaki.setWbBarcode("4673756010058");
        landingNetCaseKhaki.setPrice(new BigDecimal("1885.10"));
        landingNetCaseKhaki.setPurchasePrice(new BigDecimal("1090.00"));
        landingNetCaseKhaki.setLogisticsCost(new BigDecimal("97.29"));
        landingNetCaseKhaki.setMarketingCost(new BigDecimal("105.00"));
        landingNetCaseKhaki.setOtherExpenses(new BigDecimal("40.00"));
        landingNetCaseKhaki.setStockQuantity(36);
        landingNetCaseKhaki.setCategory("Другие принадлежности для рыбалки");
        landingNetCaseKhaki.setBrand("FISHING BAND");

        Product carpRig = new Product();
        carpRig.setName("Оснастка карповая Fishing Band флэт-метод 80 гр, №6");
        carpRig.setWbArticle("0409");
        carpRig.setPrice(new BigDecimal("679.95"));
        carpRig.setPurchasePrice(new BigDecimal("340.00"));
        carpRig.setLogisticsCost(new BigDecimal("60.80"));
        carpRig.setMarketingCost(new BigDecimal("85.00"));
        carpRig.setOtherExpenses(new BigDecimal("30.00"));
        carpRig.setStockQuantity(72);
        carpRig.setCategory("Кормушки рыболовные");
        carpRig.setBrand("FISHING BAND");

        productRepository.saveAll(List.of(landingNetCaseBlack, landingNetCaseKhaki, carpRig));
    }
}
