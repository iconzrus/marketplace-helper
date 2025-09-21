package com.marketplacehelper.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.math.BigDecimal;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class PriceRecommendationDto {

    private Long wbProductId;
    private String wbArticle;
    private String name;
    private BigDecimal currentPrice;
    private BigDecimal targetMarginPercent;
    private BigDecimal recommendedPrice;
    private BigDecimal priceDelta;

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

    public BigDecimal getCurrentPrice() {
        return currentPrice;
    }

    public void setCurrentPrice(BigDecimal currentPrice) {
        this.currentPrice = currentPrice;
    }

    public BigDecimal getTargetMarginPercent() {
        return targetMarginPercent;
    }

    public void setTargetMarginPercent(BigDecimal targetMarginPercent) {
        this.targetMarginPercent = targetMarginPercent;
    }

    public BigDecimal getRecommendedPrice() {
        return recommendedPrice;
    }

    public void setRecommendedPrice(BigDecimal recommendedPrice) {
        this.recommendedPrice = recommendedPrice;
    }

    public BigDecimal getPriceDelta() {
        return priceDelta;
    }

    public void setPriceDelta(BigDecimal priceDelta) {
        this.priceDelta = priceDelta;
    }

    public Long getWbProductId() {
        return wbProductId;
    }

    public void setWbProductId(Long wbProductId) {
        this.wbProductId = wbProductId;
    }
}


