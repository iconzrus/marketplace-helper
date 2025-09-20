package com.marketplacehelper.model;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "daily_snapshots")
public class DailySnapshot {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "snapshot_date", nullable = false)
    private LocalDate snapshotDate;

    @Column(name = "wb_article")
    private String wbArticle;

    @Column(name = "price", precision = 10, scale = 2)
    private BigDecimal price;

    @Column(name = "margin", precision = 10, scale = 2)
    private BigDecimal margin;

    @Column(name = "margin_percent", precision = 10, scale = 2)
    private BigDecimal marginPercent;

    @Column(name = "stock_local")
    private Integer stockLocal;

    @Column(name = "stock_wb")
    private Integer stockWb;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public LocalDate getSnapshotDate() {
        return snapshotDate;
    }

    public void setSnapshotDate(LocalDate snapshotDate) {
        this.snapshotDate = snapshotDate;
    }

    public String getWbArticle() {
        return wbArticle;
    }

    public void setWbArticle(String wbArticle) {
        this.wbArticle = wbArticle;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public void setPrice(BigDecimal price) {
        this.price = price;
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

    public Integer getStockLocal() {
        return stockLocal;
    }

    public void setStockLocal(Integer stockLocal) {
        this.stockLocal = stockLocal;
    }

    public Integer getStockWb() {
        return stockWb;
    }

    public void setStockWb(Integer stockWb) {
        this.stockWb = stockWb;
    }
}


