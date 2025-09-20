package com.marketplacehelper.service;

import com.marketplacehelper.dto.ProductValidationDto;
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
class ValidationServiceTest {

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
    void returnsIssuesAndSuggestionsForMissingCosts() {
        WbProduct wb = new WbProduct();
        wb.setNmId(111L);
        wb.setName("Demo");
        wb.setPrice(new BigDecimal("1000"));
        wb.setPriceWithDiscount(new BigDecimal("900"));
        wbProductRepository.saveAndFlush(wb);

        Product excel = new Product();
        excel.setName("Demo");
        excel.setWbArticle("111");
        excel.setPrice(new BigDecimal("1000"));
        // no purchase/logistics/marketing/other
        productRepository.saveAndFlush(excel);

        List<ProductValidationDto> report = analyticsService.buildValidationReport(true, new BigDecimal("10"));

        assertThat(report).hasSize(1);
        ProductValidationDto dto = report.get(0);
        assertThat(dto.isRequiresCorrection()).isTrue();
        assertThat(dto.getIssues()).extracting("field")
                .contains("purchasePrice", "logisticsCost", "marketingCost", "otherExpenses");
        assertThat(dto.getIssues()).anySatisfy(issue -> {
            if (issue.getField().equals("purchasePrice")) {
                assertThat(issue.getSuggestion()).contains("60%");
                assertThat(issue.isBlocking()).isTrue();
            }
        });
    }

    @Test
    void flagsWbOnlyWhenNoExcelData() {
        WbProduct wb = new WbProduct();
        wb.setNmId(222L);
        wb.setName("WB Only");
        wb.setPrice(new BigDecimal("500"));
        wb.setPriceWithDiscount(new BigDecimal("450"));
        wbProductRepository.saveAndFlush(wb);

        List<ProductValidationDto> report = analyticsService.buildValidationReport(true, null);
        assertThat(report).hasSize(1);
        ProductValidationDto dto = report.get(0);
        assertThat(dto.getIssues()).extracting("field").contains("excelData");
    }
}


