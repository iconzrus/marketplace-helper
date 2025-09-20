package com.marketplacehelper.dto;

public class DeleteResultDto {
    private int deletedProducts;
    private int deletedWbProducts;

    public int getDeletedProducts() {
        return deletedProducts;
    }

    public void setDeletedProducts(int deletedProducts) {
        this.deletedProducts = deletedProducts;
    }

    public int getDeletedWbProducts() {
        return deletedWbProducts;
    }

    public void setDeletedWbProducts(int deletedWbProducts) {
        this.deletedWbProducts = deletedWbProducts;
    }
}


