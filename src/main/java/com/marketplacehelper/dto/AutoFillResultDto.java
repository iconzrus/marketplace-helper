package com.marketplacehelper.dto;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class AutoFillResultDto {

    public static class AffectedItem {
        private Long productId;
        private String name;
        private String wbArticle;
        private List<String> updatedFields;

        public Long getProductId() {
            return productId;
        }

        public void setProductId(Long productId) {
            this.productId = productId;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getWbArticle() {
            return wbArticle;
        }

        public void setWbArticle(String wbArticle) {
            this.wbArticle = wbArticle;
        }

        public List<String> getUpdatedFields() {
            if (updatedFields == null) return Collections.emptyList();
            return Collections.unmodifiableList(updatedFields);
        }

        public void setUpdatedFields(List<String> updatedFields) {
            this.updatedFields = updatedFields;
        }
    }

    private int affectedCount;
    private int createdCount;
    private List<AffectedItem> items;

    public int getAffectedCount() {
        return affectedCount;
    }

    public void setAffectedCount(int affectedCount) {
        this.affectedCount = affectedCount;
    }

    public int getCreatedCount() {
        return createdCount;
    }

    public void setCreatedCount(int createdCount) {
        this.createdCount = createdCount;
    }

    public List<AffectedItem> getItems() {
        if (items == null) return Collections.emptyList();
        return Collections.unmodifiableList(items);
    }

    public void setItems(List<AffectedItem> items) {
        this.items = items;
    }

    public void addItem(AffectedItem item) {
        if (item == null) return;
        if (this.items == null) this.items = new ArrayList<>();
        this.items.add(item);
    }
}


