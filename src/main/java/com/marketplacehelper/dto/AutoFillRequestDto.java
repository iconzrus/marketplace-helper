package com.marketplacehelper.dto;

import java.math.BigDecimal;

public class AutoFillRequestDto {

    private BigDecimal purchasePercentOfPrice; // e.g. 60 -> 60% of price
    private BigDecimal logisticsFixed;         // e.g. 70 RUB per unit
    private BigDecimal marketingPercentOfPrice; // e.g. 8 -> 8% of price
    private BigDecimal otherFixed;             // e.g. 0
    private boolean onlyIfMissing = true;
    private Integer limit;                     // optional, max affected products

    public BigDecimal getPurchasePercentOfPrice() {
        return purchasePercentOfPrice;
    }

    public void setPurchasePercentOfPrice(BigDecimal purchasePercentOfPrice) {
        this.purchasePercentOfPrice = purchasePercentOfPrice;
    }

    public BigDecimal getLogisticsFixed() {
        return logisticsFixed;
    }

    public void setLogisticsFixed(BigDecimal logisticsFixed) {
        this.logisticsFixed = logisticsFixed;
    }

    public BigDecimal getMarketingPercentOfPrice() {
        return marketingPercentOfPrice;
    }

    public void setMarketingPercentOfPrice(BigDecimal marketingPercentOfPrice) {
        this.marketingPercentOfPrice = marketingPercentOfPrice;
    }

    public BigDecimal getOtherFixed() {
        return otherFixed;
    }

    public void setOtherFixed(BigDecimal otherFixed) {
        this.otherFixed = otherFixed;
    }

    public boolean isOnlyIfMissing() {
        return onlyIfMissing;
    }

    public void setOnlyIfMissing(boolean onlyIfMissing) {
        this.onlyIfMissing = onlyIfMissing;
    }

    public Integer getLimit() {
        return limit;
    }

    public void setLimit(Integer limit) {
        this.limit = limit;
    }
}


