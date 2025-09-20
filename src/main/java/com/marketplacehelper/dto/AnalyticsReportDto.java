package com.marketplacehelper.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.math.BigDecimal;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class AnalyticsReportDto {

    private List<ProductAnalyticsDto> profitable;
    private List<ProductAnalyticsDto> requiresAttention;
    private List<ProductAnalyticsDto> allItems;
    private BigDecimal appliedMinMarginPercent;
    private long totalProducts;
    private long profitableCount;
    private long requiresAttentionCount;

    public List<ProductAnalyticsDto> getProfitable() {
        return profitable;
    }

    public void setProfitable(List<ProductAnalyticsDto> profitable) {
        this.profitable = profitable;
    }

    public List<ProductAnalyticsDto> getRequiresAttention() {
        return requiresAttention;
    }

    public void setRequiresAttention(List<ProductAnalyticsDto> requiresAttention) {
        this.requiresAttention = requiresAttention;
    }

    public List<ProductAnalyticsDto> getAllItems() {
        return allItems;
    }

    public void setAllItems(List<ProductAnalyticsDto> allItems) {
        this.allItems = allItems;
    }

    public BigDecimal getAppliedMinMarginPercent() {
        return appliedMinMarginPercent;
    }

    public void setAppliedMinMarginPercent(BigDecimal appliedMinMarginPercent) {
        this.appliedMinMarginPercent = appliedMinMarginPercent;
    }

    public long getTotalProducts() {
        return totalProducts;
    }

    public void setTotalProducts(long totalProducts) {
        this.totalProducts = totalProducts;
    }

    public long getProfitableCount() {
        return profitableCount;
    }

    public void setProfitableCount(long profitableCount) {
        this.profitableCount = profitableCount;
    }

    public long getRequiresAttentionCount() {
        return requiresAttentionCount;
    }

    public void setRequiresAttentionCount(long requiresAttentionCount) {
        this.requiresAttentionCount = requiresAttentionCount;
    }
}
