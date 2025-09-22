package com.marketplacehelper.dto;

import java.math.BigDecimal;
import java.util.List;

/**
 * Запрос на массовое обновление цен. Поддерживает как обновление локальных Product, так и WB товары.
 */
public class BatchPriceUpdateRequest {

    public static class Item {
        private Long productId;
        private Long wbProductId;
        private String wbArticle;
        private BigDecimal newPrice;

        public Long getProductId() { return productId; }
        public void setProductId(Long productId) { this.productId = productId; }

        public Long getWbProductId() { return wbProductId; }
        public void setWbProductId(Long wbProductId) { this.wbProductId = wbProductId; }

        public String getWbArticle() { return wbArticle; }
        public void setWbArticle(String wbArticle) { this.wbArticle = wbArticle; }

        public BigDecimal getNewPrice() { return newPrice; }
        public void setNewPrice(BigDecimal newPrice) { this.newPrice = newPrice; }
    }

    private List<Item> items;
    private Boolean dryRun;
    private String roundingRule; // NONE | NEAREST_1 | NEAREST_5 | NEAREST_10
    private java.math.BigDecimal floorPrice;
    private java.math.BigDecimal ceilPrice;
    private java.math.BigDecimal maxDeltaPercent;

    public List<Item> getItems() { return items; }
    public void setItems(List<Item> items) { this.items = items; }

    public Boolean getDryRun() { return dryRun; }
    public void setDryRun(Boolean dryRun) { this.dryRun = dryRun; }

    public String getRoundingRule() { return roundingRule; }
    public void setRoundingRule(String roundingRule) { this.roundingRule = roundingRule; }

    public java.math.BigDecimal getFloorPrice() { return floorPrice; }
    public void setFloorPrice(java.math.BigDecimal floorPrice) { this.floorPrice = floorPrice; }

    public java.math.BigDecimal getCeilPrice() { return ceilPrice; }
    public void setCeilPrice(java.math.BigDecimal ceilPrice) { this.ceilPrice = ceilPrice; }

    public java.math.BigDecimal getMaxDeltaPercent() { return maxDeltaPercent; }
    public void setMaxDeltaPercent(java.math.BigDecimal maxDeltaPercent) { this.maxDeltaPercent = maxDeltaPercent; }
}



