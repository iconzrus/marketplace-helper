package com.marketplacehelper.service;

import com.marketplacehelper.dto.AnalyticsReportDto;
import com.marketplacehelper.dto.ProductAnalyticsDto;
import com.marketplacehelper.model.DailySnapshot;
import com.marketplacehelper.repository.DailySnapshotRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Service
public class SnapshotService {

    private final AnalyticsService analyticsService;
    private final DailySnapshotRepository snapshotRepository;

    public SnapshotService(AnalyticsService analyticsService, DailySnapshotRepository snapshotRepository) {
        this.analyticsService = analyticsService;
        this.snapshotRepository = snapshotRepository;
    }

    public void takeDailySnapshot(LocalDate date) {
        AnalyticsReportDto analytics = analyticsService.buildProductAnalyticsReport(true, null, true);
        List<DailySnapshot> snapshots = new ArrayList<>();
        for (ProductAnalyticsDto item : analytics.getAllItems()) {
            DailySnapshot s = new DailySnapshot();
            s.setSnapshotDate(date);
            s.setWbArticle(item.getWbArticle());
            s.setPrice(item.getWbDiscountPrice() != null ? item.getWbDiscountPrice() : item.getWbPrice());
            s.setMargin(item.getMargin());
            s.setMarginPercent(item.getMarginPercent());
            s.setStockLocal(item.getLocalStock());
            s.setStockWb(item.getWbStock());
            snapshots.add(s);
        }
        snapshotRepository.saveAll(snapshots);
    }

    public List<DailySnapshot> getSnapshots(LocalDate from, LocalDate to) {
        return snapshotRepository.findAllBetween(from, to);
    }
}


