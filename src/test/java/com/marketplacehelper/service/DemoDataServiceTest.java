package com.marketplacehelper.service;

import com.marketplacehelper.dto.AutoFillRequestDto;
import com.marketplacehelper.dto.AutoFillResultDto;
import com.marketplacehelper.model.Product;
import com.marketplacehelper.repository.ProductRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@Import(DemoDataService.class)
class DemoDataServiceTest {

    @Autowired
    private DemoDataService demoDataService;

    @Autowired
    private ProductRepository productRepository;

    @BeforeEach
    void clean() {
        productRepository.deleteAll();
    }

    @Test
    void fillsMissingCostsUsingRules() {
        Product p = new Product();
        p.setName("Demo");
        p.setWbArticle("111");
        p.setPrice(new BigDecimal("1000"));
        productRepository.saveAndFlush(p);

        AutoFillRequestDto req = new AutoFillRequestDto();
        req.setPurchasePercentOfPrice(new BigDecimal("60"));
        req.setLogisticsFixed(new BigDecimal("70"));
        req.setMarketingPercentOfPrice(new BigDecimal("8"));
        req.setOtherFixed(new BigDecimal("0"));
        req.setOnlyIfMissing(true);

        AutoFillResultDto result = demoDataService.autoFillMissingCosts(req);

        assertThat(result.getAffectedCount()).isEqualTo(1);
        Product updated = productRepository.findAll().get(0);
        assertThat(updated.getPurchasePrice()).isEqualByComparingTo(new BigDecimal("600.00"));
        assertThat(updated.getLogisticsCost()).isEqualByComparingTo(new BigDecimal("70"));
        assertThat(updated.getMarketingCost()).isEqualByComparingTo(new BigDecimal("80.00"));
        assertThat(updated.getOtherExpenses()).isEqualByComparingTo(new BigDecimal("0"));
    }

    @Test
    void fillRandomAllCompletesMergingAndPopulatesFields() {
        Product p = new Product();
        p.setName("Excel Only");
        p.setPrice(new BigDecimal("1200"));
        productRepository.saveAndFlush(p);

        int affected = demoDataService.fillRandomAll();
        assertThat(affected).isGreaterThan(0);

        Product updated = productRepository.findAll().get(0);
        assertThat(updated.getWbArticle()).isNotBlank();
        assertThat(updated.getPurchasePrice()).isNotNull();
        assertThat(updated.getLogisticsCost()).isNotNull();
        assertThat(updated.getMarketingCost()).isNotNull();
        assertThat(updated.getOtherExpenses()).isNotNull();
    }

    @Test
    void generateCreatesRequestedCounts() {
        int createdBoth = demoDataService.generateDemo(7, "both");
        assertThat(createdBoth).isEqualTo(7);

        int createdExcel = demoDataService.generateDemo(5, "excel");
        assertThat(createdExcel).isEqualTo(5);

        int createdWb = demoDataService.generateDemo(4, "wb");
        assertThat(createdWb).isEqualTo(4);
    }

    @Test
    void deleteRandomAndAllWork() {
        demoDataService.generateDemo(6, "both");
        com.marketplacehelper.dto.DeleteResultDto partial = demoDataService.deleteRandom(3, false);
        assertThat(partial.getDeletedProducts() + partial.getDeletedWbProducts()).isGreaterThan(0);

        com.marketplacehelper.dto.DeleteResultDto all = demoDataService.deleteRandom(0, true);
        assertThat(all.getDeletedProducts()).isGreaterThanOrEqualTo(0);
        assertThat(all.getDeletedWbProducts()).isGreaterThanOrEqualTo(0);
    }
}


