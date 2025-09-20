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
        smartphone.setSupplierArticle("100001");
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
        headphones.setSupplierArticle("WAVE-AIR");
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
        backpack.setSupplierArticle("100003");
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
        smartwatch.setSupplierArticle("100004");
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
        blender.setSupplierArticle("KP-900");
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
        sneakers.setSupplierArticle("100007");
        sneakers.setPrice(new BigDecimal("6290"));
        sneakers.setPurchasePrice(new BigDecimal("3600"));
        sneakers.setLogisticsCost(new BigDecimal("410"));
        sneakers.setMarketingCost(new BigDecimal("320"));
        sneakers.setOtherExpenses(new BigDecimal("260"));
        sneakers.setStockQuantity(90);
        sneakers.setCategory("Спорт");
        sneakers.setBrand("FastRun");

        Product fishingCaseBlackLogistics = new Product();
        fishingCaseBlackLogistics.setName("Чехол для рыболовного подсака 80*85 см чёрный — логистика поставки 29891778");
        fishingCaseBlackLogistics.setWbArticle("ch5_bl-logistics-db_r50b6465426b64a2ba203ed4943862bc0_2_0");
        fishingCaseBlackLogistics.setSupplierArticle("ch5_bl");
        fishingCaseBlackLogistics.setWbBarcode("4673756010669");
        fishingCaseBlackLogistics.setPrice(new BigDecimal("76.00"));
        fishingCaseBlackLogistics.setPurchasePrice(new BigDecimal("1180.00"));
        fishingCaseBlackLogistics.setLogisticsCost(new BigDecimal("76.00"));
        fishingCaseBlackLogistics.setMarketingCost(new BigDecimal("0.00"));
        fishingCaseBlackLogistics.setOtherExpenses(new BigDecimal("0.00"));
        fishingCaseBlackLogistics.setStockQuantity(0);
        fishingCaseBlackLogistics.setCategory("Другие принадлежности для рыбалки");
        fishingCaseBlackLogistics.setBrand("FISHING BAND");

        Product fishingCaseBlackSale = new Product();
        fishingCaseBlackSale.setName("Чехол для рыболовного подсака 80*85 см чёрный — продажа 01.09.2025");
        fishingCaseBlackSale.setWbArticle("ch5_bl-sale-db_r50b6465426b64a2ba203ed4943862bc0_2_0");
        fishingCaseBlackSale.setSupplierArticle("ch5_bl");
        fishingCaseBlackSale.setWbBarcode("4673756010669");
        fishingCaseBlackSale.setPrice(new BigDecimal("2059.55"));
        fishingCaseBlackSale.setPurchasePrice(new BigDecimal("1180.00"));
        fishingCaseBlackSale.setLogisticsCost(new BigDecimal("76.00"));
        fishingCaseBlackSale.setMarketingCost(new BigDecimal("120.00"));
        fishingCaseBlackSale.setOtherExpenses(new BigDecimal("45.00"));
        fishingCaseBlackSale.setStockQuantity(1);
        fishingCaseBlackSale.setCategory("Другие принадлежности для рыбалки");
        fishingCaseBlackSale.setBrand("FISHING BAND");

        Product fishingCaseKhakiLogistics = new Product();
        fishingCaseKhakiLogistics.setName("Чехол для подсака и подсачека непромокаемый 80*85 см хаки — логистика поставки");
        fishingCaseKhakiLogistics.setWbArticle("ch1-logistics-dn_rba164bef11a746ff829e3d577be40a49_0_0");
        fishingCaseKhakiLogistics.setSupplierArticle("ch1");
        fishingCaseKhakiLogistics.setWbBarcode("4673756010058");
        fishingCaseKhakiLogistics.setPrice(new BigDecimal("97.29"));
        fishingCaseKhakiLogistics.setPurchasePrice(new BigDecimal("1090.00"));
        fishingCaseKhakiLogistics.setLogisticsCost(new BigDecimal("97.29"));
        fishingCaseKhakiLogistics.setMarketingCost(new BigDecimal("0.00"));
        fishingCaseKhakiLogistics.setOtherExpenses(new BigDecimal("0.00"));
        fishingCaseKhakiLogistics.setStockQuantity(0);
        fishingCaseKhakiLogistics.setCategory("Другие принадлежности для рыбалки");
        fishingCaseKhakiLogistics.setBrand("FISHING BAND");

        Product fishingCaseKhakiSale = new Product();
        fishingCaseKhakiSale.setName("Чехол для подсака и подсачека непромокаемый 80*85 см хаки — продажа 06.09.2025");
        fishingCaseKhakiSale.setWbArticle("ch1-sale-dn_rba164bef11a746ff829e3d577be40a49_0_0");
        fishingCaseKhakiSale.setSupplierArticle("ch1");
        fishingCaseKhakiSale.setWbBarcode("4673756010058");
        fishingCaseKhakiSale.setPrice(new BigDecimal("1885.10"));
        fishingCaseKhakiSale.setPurchasePrice(new BigDecimal("1090.00"));
        fishingCaseKhakiSale.setLogisticsCost(new BigDecimal("97.29"));
        fishingCaseKhakiSale.setMarketingCost(new BigDecimal("105.00"));
        fishingCaseKhakiSale.setOtherExpenses(new BigDecimal("40.00"));
        fishingCaseKhakiSale.setStockQuantity(1);
        fishingCaseKhakiSale.setCategory("Другие принадлежности для рыбалки");
        fishingCaseKhakiSale.setBrand("FISHING BAND");

        Product carpRigLogisticsFirst = new Product();
        carpRigLogisticsFirst.setName("Оснастка карповая Fishing Band флэт-метод 80 гр, №6 — логистика заказ 41099361170");
        carpRigLogisticsFirst.setWbArticle("0409-logistics-186652897_6312059407594780177");
        carpRigLogisticsFirst.setSupplierArticle("0409");
        carpRigLogisticsFirst.setPrice(new BigDecimal("60.80"));
        carpRigLogisticsFirst.setPurchasePrice(new BigDecimal("340.00"));
        carpRigLogisticsFirst.setLogisticsCost(new BigDecimal("60.80"));
        carpRigLogisticsFirst.setMarketingCost(new BigDecimal("0.00"));
        carpRigLogisticsFirst.setOtherExpenses(new BigDecimal("0.00"));
        carpRigLogisticsFirst.setStockQuantity(0);
        carpRigLogisticsFirst.setCategory("Кормушки рыболовные");
        carpRigLogisticsFirst.setBrand("FISHING BAND");

        Product carpRigSaleFirst = new Product();
        carpRigSaleFirst.setName("Оснастка карповая Fishing Band флэт-метод 80 гр, №6 — продажа 06.09.2025 (заказ 41099361170)");
        carpRigSaleFirst.setWbArticle("0409-sale-186652897_6312059407594780177");
        carpRigSaleFirst.setSupplierArticle("0409");
        carpRigSaleFirst.setPrice(new BigDecimal("679.95"));
        carpRigSaleFirst.setPurchasePrice(new BigDecimal("340.00"));
        carpRigSaleFirst.setLogisticsCost(new BigDecimal("60.80"));
        carpRigSaleFirst.setMarketingCost(new BigDecimal("85.00"));
        carpRigSaleFirst.setOtherExpenses(new BigDecimal("30.00"));
        carpRigSaleFirst.setStockQuantity(1);
        carpRigSaleFirst.setCategory("Кормушки рыболовные");
        carpRigSaleFirst.setBrand("FISHING BAND");

        Product carpRigLogisticsSecond = new Product();
        carpRigLogisticsSecond.setName("Оснастка карповая Fishing Band флэт-метод 80 гр, №6 — логистика заказ 41107790736");
        carpRigLogisticsSecond.setWbArticle("0409-logistics-186652897_8262909513171099095");
        carpRigLogisticsSecond.setSupplierArticle("0409");
        carpRigLogisticsSecond.setPrice(new BigDecimal("60.80"));
        carpRigLogisticsSecond.setPurchasePrice(new BigDecimal("340.00"));
        carpRigLogisticsSecond.setLogisticsCost(new BigDecimal("60.80"));
        carpRigLogisticsSecond.setMarketingCost(new BigDecimal("0.00"));
        carpRigLogisticsSecond.setOtherExpenses(new BigDecimal("0.00"));
        carpRigLogisticsSecond.setStockQuantity(0);
        carpRigLogisticsSecond.setCategory("Кормушки рыболовные");
        carpRigLogisticsSecond.setBrand("FISHING BAND");

        Product carpRigSaleSecond = new Product();
        carpRigSaleSecond.setName("Оснастка карповая Fishing Band флэт-метод 80 гр, №6 — продажа 06.09.2025 (заказ 41107790736)");
        carpRigSaleSecond.setWbArticle("0409-sale-186652897_8262909513171099095");
        carpRigSaleSecond.setSupplierArticle("0409");
        carpRigSaleSecond.setPrice(new BigDecimal("679.95"));
        carpRigSaleSecond.setPurchasePrice(new BigDecimal("340.00"));
        carpRigSaleSecond.setLogisticsCost(new BigDecimal("60.80"));
        carpRigSaleSecond.setMarketingCost(new BigDecimal("85.00"));
        carpRigSaleSecond.setOtherExpenses(new BigDecimal("30.00"));
        carpRigSaleSecond.setStockQuantity(1);
        carpRigSaleSecond.setCategory("Кормушки рыболовные");
        carpRigSaleSecond.setBrand("FISHING BAND");

        productRepository.saveAll(List.of(
                smartphone,
                headphones,
                backpack,
                smartwatch,
                blender,
                sneakers,
                fishingCaseBlackLogistics,
                fishingCaseBlackSale,
                fishingCaseKhakiLogistics,
                fishingCaseKhakiSale,
                carpRigLogisticsFirst,
                carpRigSaleFirst,
                carpRigLogisticsSecond,
                carpRigSaleSecond
        ));
    }
}
