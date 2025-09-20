package com.marketplacehelper.dto;

import java.math.BigDecimal;

public class UpdateCostsRequest {
    private BigDecimal purchasePrice;
    private BigDecimal logisticsCost;
    private BigDecimal marketingCost;
    private BigDecimal otherExpenses;

    public BigDecimal getPurchasePrice() { return purchasePrice; }
    public void setPurchasePrice(BigDecimal purchasePrice) { this.purchasePrice = purchasePrice; }

    public BigDecimal getLogisticsCost() { return logisticsCost; }
    public void setLogisticsCost(BigDecimal logisticsCost) { this.logisticsCost = logisticsCost; }

    public BigDecimal getMarketingCost() { return marketingCost; }
    public void setMarketingCost(BigDecimal marketingCost) { this.marketingCost = marketingCost; }

    public BigDecimal getOtherExpenses() { return otherExpenses; }
    public void setOtherExpenses(BigDecimal otherExpenses) { this.otherExpenses = otherExpenses; }
}


