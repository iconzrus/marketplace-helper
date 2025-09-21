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

    public List<Item> getItems() { return items; }
    public void setItems(List<Item> items) { this.items = items; }
}



