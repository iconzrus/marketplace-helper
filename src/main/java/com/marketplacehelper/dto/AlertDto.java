package com.marketplacehelper.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.math.BigDecimal;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class AlertDto {

    public enum AlertType { LOW_MARGIN, NEGATIVE_MARGIN, LOW_STOCK }

    private AlertType type;
    private String wbArticle;
    private String name;
    private BigDecimal margin;
    private BigDecimal marginPercent;
    private Integer localStock;
    private Integer wbStock;
    private BigDecimal currentPrice;

    public AlertType getType() {
        return type;
    }

    public void setType(AlertType type) {
        this.type = type;
    }

    public String getWbArticle() {
        return wbArticle;
    }

    public void setWbArticle(String wbArticle) {
        this.wbArticle = wbArticle;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
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

    public BigDecimal getCurrentPrice() {
        return currentPrice;
    }

    public void setCurrentPrice(BigDecimal currentPrice) {
        this.currentPrice = currentPrice;
    }
}


