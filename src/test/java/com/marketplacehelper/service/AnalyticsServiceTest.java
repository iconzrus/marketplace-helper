package com.marketplacehelper.service;

import com.marketplacehelper.dto.AnalyticsReportDto;
import com.marketplacehelper.dto.ProductAnalyticsDto;
import com.marketplacehelper.dto.ProductDataSource;
import com.marketplacehelper.model.Product;
import com.marketplacehelper.model.WbProduct;
import com.marketplacehelper.repository.ProductRepository;
import com.marketplacehelper.repository.WbProductRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@Import(AnalyticsService.class)
@TestPropertySource(properties = {
        "app.analytics.min-margin-percent=10",
        "app.analytics.filter-negative-margin=true"
})
class AnalyticsServiceTest {

    @Autowired
    private AnalyticsService analyticsService;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private WbProductRepository wbProductRepository;

    @BeforeEach
    void cleanUp() {
        productRepository.deleteAll();
        wbProductRepository.deleteAll();
    }

    @Test
    void shouldMergeWildberriesCardWithExcelDataWithoutWarnings() {
        WbProduct wbProduct = buildFishingCover();
        wbProductRepository.saveAndFlush(wbProduct);

        AnalyticsReportDto initialReport = analyticsService.buildProductAnalyticsReport(true, null, true);
        assertThat(initialReport.getAllItems()).hasSize(1);
        ProductAnalyticsDto wbOnlyDto = initialReport.getAllItems().get(0);
        assertThat(wbOnlyDto.getDataSource()).isEqualTo(ProductDataSource.WB_ONLY);
        assertThat(wbOnlyDto.isRequiresCorrection()).isTrue();

        Product excelProduct = buildExcelProduct();
        productRepository.saveAndFlush(excelProduct);

        AnalyticsReportDto mergedReport = analyticsService.buildProductAnalyticsReport(true, null, true);
        List<ProductAnalyticsDto> items = mergedReport.getAllItems();
        assertThat(items).hasSize(1);

        ProductAnalyticsDto dto = items.get(0);
        assertThat(dto.getDataSource()).isEqualTo(ProductDataSource.MERGED);
        assertThat(dto.isRequiresCorrection()).isFalse();
        assertThat(dto.isProfitable()).isTrue();
        assertThat(dto.getWarnings()).isEmpty();

        assertThat(mergedReport.getProfitable()).hasSize(1);
        assertThat(mergedReport.getRequiresAttention()).isEmpty();
    }

    @Test
    void shouldNotDuplicateWildberriesCardWhenArticleContainsLeadingZeros() {
        WbProduct wbProduct = buildFishingCover();
        wbProductRepository.saveAndFlush(wbProduct);

        Product excelProduct = buildExcelProduct();
        excelProduct.setWbArticle("00186961443");
        productRepository.saveAndFlush(excelProduct);

        AnalyticsReportDto report = analyticsService.buildProductAnalyticsReport(true, null, true);

        assertThat(report.getAllItems()).hasSize(1);
        ProductAnalyticsDto dto = report.getAllItems().get(0);
        assertThat(dto.getDataSource()).isEqualTo(ProductDataSource.MERGED);
        assertThat(dto.isRequiresCorrection()).isFalse();
        assertThat(dto.isProfitable()).isTrue();
        assertThat(report.getRequiresAttention()).isEmpty();
    }

    private WbProduct buildFishingCover() {
        WbProduct wbProduct = new WbProduct();
        wbProduct.setNmId(186961443L);
        wbProduct.setName("Чехол для рыболовного подсака 80×85 см чёрный");
        wbProduct.setVendor("FISHING BAND");
        wbProduct.setVendorCode("ch5_bl");
        wbProduct.setBrand("FISHING BAND");
        wbProduct.setCategory("Другие принадлежности для рыбалки");
        wbProduct.setPrice(new BigDecimal("2059.55"));
        wbProduct.setPriceWithDiscount(new BigDecimal("1692.26"));
        wbProduct.setSalePrice(new BigDecimal("2059.55"));
        wbProduct.setSale(25);
        wbProduct.setBasicSale(10);
        wbProduct.setBasicPriceU(new BigDecimal("2059.55"));
        wbProduct.setTotalQuantity(76);
        wbProduct.setQuantityNotInOrders(18);
        wbProduct.setQuantityFull(94);
        wbProduct.setInWayToClient(6);
        wbProduct.setInWayFromClient(1);
        wbProduct.setColors("Черный");
        wbProduct.setSizes("80×85");
        return wbProduct;
    }

    private Product buildExcelProduct() {
        Product product = new Product();
        product.setName("Чехол для рыболовного подсака 80*85 см чёрный");
        product.setWbArticle("186961443");
        product.setBrand("FISHING BAND");
        product.setCategory("Другие принадлежности для рыбалки");
        product.setPrice(new BigDecimal("2059.55"));
        product.setPurchasePrice(new BigDecimal("950"));
        product.setLogisticsCost(new BigDecimal("210"));
        product.setMarketingCost(new BigDecimal("130"));
        product.setOtherExpenses(new BigDecimal("80"));
        product.setStockQuantity(12);
        return product;
    }
}
