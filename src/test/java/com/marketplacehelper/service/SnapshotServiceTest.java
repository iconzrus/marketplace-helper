package com.marketplacehelper.service;

import com.marketplacehelper.dto.AnalyticsReportDto;
import com.marketplacehelper.dto.ProductAnalyticsDto;
import com.marketplacehelper.model.DailySnapshot;
import com.marketplacehelper.repository.DailySnapshotRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@DataJpaTest
@Import({SnapshotService.class})
class SnapshotServiceTest {

    @Autowired
    private SnapshotService snapshotService;

    @Autowired
    private DailySnapshotRepository snapshotRepository;

    @MockBean
    private AnalyticsService analyticsService;

    @Test
    void shouldPersistDailySnapshotsFromAnalytics() {
        ProductAnalyticsDto item = new ProductAnalyticsDto();
        item.setName("SKU");
        item.setWbArticle("100");
        item.setWbPrice(new BigDecimal("1000"));
        item.setMargin(new BigDecimal("300"));
        item.setMarginPercent(new BigDecimal("30"));
        item.setLocalStock(5);
        item.setWbStock(7);

        AnalyticsReportDto report = new AnalyticsReportDto();
        report.setAllItems(List.of(item));
        when(analyticsService.buildProductAnalyticsReport(true, null, true)).thenReturn(report);

        LocalDate today = LocalDate.of(2025, 1, 1);
        snapshotService.takeDailySnapshot(today);

        List<DailySnapshot> stored = snapshotRepository.findAll();
        assertThat(stored).hasSize(1);
        DailySnapshot s = stored.get(0);
        assertThat(s.getSnapshotDate()).isEqualTo(today);
        assertThat(s.getWbArticle()).isEqualTo("100");
        assertThat(s.getPrice()).isEqualByComparingTo("1000");
        assertThat(s.getMarginPercent()).isEqualByComparingTo("30");
        assertThat(s.getStockLocal()).isEqualTo(5);
        assertThat(s.getStockWb()).isEqualTo(7);
    }
}


