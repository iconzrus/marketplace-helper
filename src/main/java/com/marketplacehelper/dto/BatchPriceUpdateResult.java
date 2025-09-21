package com.marketplacehelper.dto;

import java.util.ArrayList;
import java.util.List;

public class BatchPriceUpdateResult {
    public static class ResultItem {
        private Long productId;
        private Long wbProductId;
        private String wbArticle;
        private boolean success;
        private String message;

        public Long getProductId() { return productId; }
        public void setProductId(Long productId) { this.productId = productId; }

        public Long getWbProductId() { return wbProductId; }
        public void setWbProductId(Long wbProductId) { this.wbProductId = wbProductId; }

        public String getWbArticle() { return wbArticle; }
        public void setWbArticle(String wbArticle) { this.wbArticle = wbArticle; }

        public boolean isSuccess() { return success; }
        public void setSuccess(boolean success) { this.success = success; }

        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }
    }

    private int updated;
    private int failed;
    private List<ResultItem> items = new ArrayList<>();

    public int getUpdated() { return updated; }
    public void setUpdated(int updated) { this.updated = updated; }

    public int getFailed() { return failed; }
    public void setFailed(int failed) { this.failed = failed; }

    public List<ResultItem> getItems() { return items; }
    public void setItems(List<ResultItem> items) { this.items = items; }
}



