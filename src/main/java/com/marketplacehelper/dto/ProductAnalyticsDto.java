package com.marketplacehelper.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.math.BigDecimal;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class ProductAnalyticsDto {

    private Long productId;
    private Long wbProductId;
    private String name;
    private String wbArticle;
    private String vendorCode;
    private String brand;
    private String category;
    private BigDecimal localPrice;
    private BigDecimal wbPrice;
    private BigDecimal wbDiscountPrice;
    private BigDecimal purchasePrice;
    private BigDecimal logisticsCost;
    private BigDecimal marketingCost;
    private BigDecimal otherExpenses;
    private Integer localStock;
    private Integer wbStock;
    private BigDecimal margin;
    private BigDecimal marginPercent;

    public Long getProductId() {
        return productId;
    }

    public void setProductId(Long productId) {
        this.productId = productId;
    }

    public Long getWbProductId() {
        return wbProductId;
    }

    public void setWbProductId(Long wbProductId) {
        this.wbProductId = wbProductId;
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

    public String getVendorCode() {
        return vendorCode;
    }

    public void setVendorCode(String vendorCode) {
        this.vendorCode = vendorCode;
    }

    public String getBrand() {
        return brand;
    }

    public void setBrand(String brand) {
        this.brand = brand;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public BigDecimal getLocalPrice() {
        return localPrice;
    }

    public void setLocalPrice(BigDecimal localPrice) {
        this.localPrice = localPrice;
    }

    public BigDecimal getWbPrice() {
        return wbPrice;
    }

    public void setWbPrice(BigDecimal wbPrice) {
        this.wbPrice = wbPrice;
    }

    public BigDecimal getWbDiscountPrice() {
        return wbDiscountPrice;
    }

    public void setWbDiscountPrice(BigDecimal wbDiscountPrice) {
        this.wbDiscountPrice = wbDiscountPrice;
    }

    public BigDecimal getPurchasePrice() {
        return purchasePrice;
    }

    public void setPurchasePrice(BigDecimal purchasePrice) {
        this.purchasePrice = purchasePrice;
    }

    public BigDecimal getLogisticsCost() {
        return logisticsCost;
    }

    public void setLogisticsCost(BigDecimal logisticsCost) {
        this.logisticsCost = logisticsCost;
    }

    public BigDecimal getMarketingCost() {
        return marketingCost;
    }

    public void setMarketingCost(BigDecimal marketingCost) {
        this.marketingCost = marketingCost;
    }

    public BigDecimal getOtherExpenses() {
        return otherExpenses;
    }

    public void setOtherExpenses(BigDecimal otherExpenses) {
        this.otherExpenses = otherExpenses;
    }

    public Integer getLocalStock() {
        return localStock;
    }

    public void setLocalStock(Integer localStock) {
        this.localStock = localStock;
    }

    public Integer getWbStock() {
        return wbStock;
    }

    public void setWbStock(Integer wbStock) {
        this.wbStock = wbStock;
    }

    public BigDecimal getMargin() {
        return margin;
    }

    public void setMargin(BigDecimal margin) {
        this.margin = margin;
    }

    public BigDecimal getMarginPercent() {
        return marginPercent;
    }

    public void setMarginPercent(BigDecimal marginPercent) {
        this.marginPercent = marginPercent;
    }
}
