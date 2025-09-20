package com.marketplacehelper.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "wb_products")
public class WbProduct {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @JsonProperty("nm_id")
    @Column(name = "nm_id", unique = true)
    private Long nmId; // Артикул WB
    
    @JsonProperty("name")
    @Column(name = "name")
    private String name;
    
    @JsonProperty("vendor")
    @Column(name = "vendor")
    private String vendor;
    
    @JsonProperty("vendor_code")
    @Column(name = "vendor_code")
    private String vendorCode;
    
    @JsonProperty("price")
    @Column(name = "price", precision = 10, scale = 2)
    private BigDecimal price;
    
    @JsonProperty("discount")
    @Column(name = "discount")
    private Integer discount;
    
    @JsonProperty("price_with_discount")
    @Column(name = "price_with_discount", precision = 10, scale = 2)
    private BigDecimal priceWithDiscount;
    
    @JsonProperty("sale_price")
    @Column(name = "sale_price", precision = 10, scale = 2)
    private BigDecimal salePrice;
    
    @JsonProperty("sale")
    @Column(name = "sale")
    private Integer sale;
    
    @JsonProperty("basic_sale")
    @Column(name = "basic_sale")
    private Integer basicSale;
    
    @JsonProperty("basic_price_u")
    @Column(name = "basic_price_u", precision = 10, scale = 2)
    private BigDecimal basicPriceU;
    
    @JsonProperty("total_quantity")
    @Column(name = "total_quantity")
    private Integer totalQuantity;
    
    @JsonProperty("quantity_not_in_orders")
    @Column(name = "quantity_not_in_orders")
    private Integer quantityNotInOrders;
    
    @JsonProperty("quantity_full")
    @Column(name = "quantity_full")
    private Integer quantityFull;
    
    @JsonProperty("in_way_to_client")
    @Column(name = "in_way_to_client")
    private Integer inWayToClient;
    
    @JsonProperty("in_way_from_client")
    @Column(name = "in_way_from_client")
    private Integer inWayFromClient;
    
    @JsonProperty("category")
    @Column(name = "category")
    private String category;
    
    @JsonProperty("subject")
    @Column(name = "subject")
    private String subject;
    
    @JsonProperty("brand")
    @Column(name = "brand")
    private String brand;
    
    @JsonProperty("colors")
    @Column(name = "colors")
    private String colors;
    
    @JsonProperty("sizes")
    @Column(name = "sizes")
    private String sizes;
    
    @Column(name = "created_at")
    private LocalDateTime createdAt;
    
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    // Конструкторы
    public WbProduct() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }
    
    // Геттеры и сеттеры
    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
    }
    
    public Long getNmId() {
        return nmId;
    }
    
    public void setNmId(Long nmId) {
        this.nmId = nmId;
    }
    
    public String getName() {
        return name;
    }
    
    public void setName(String name) {
        this.name = name;
    }
    
    public String getVendor() {
        return vendor;
    }
    
    public void setVendor(String vendor) {
        this.vendor = vendor;
    }
    
    public String getVendorCode() {
        return vendorCode;
    }
    
    public void setVendorCode(String vendorCode) {
        this.vendorCode = vendorCode;
    }
    
    public BigDecimal getPrice() {
        return price;
    }
    
    public void setPrice(BigDecimal price) {
        this.price = price;
    }
    
    public Integer getDiscount() {
        return discount;
    }
    
    public void setDiscount(Integer discount) {
        this.discount = discount;
    }
    
    public BigDecimal getPriceWithDiscount() {
        return priceWithDiscount;
    }
    
    public void setPriceWithDiscount(BigDecimal priceWithDiscount) {
        this.priceWithDiscount = priceWithDiscount;
    }
    
    public BigDecimal getSalePrice() {
        return salePrice;
    }
    
    public void setSalePrice(BigDecimal salePrice) {
        this.salePrice = salePrice;
    }
    
    public Integer getSale() {
        return sale;
    }
    
    public void setSale(Integer sale) {
        this.sale = sale;
    }
    
    public Integer getBasicSale() {
        return basicSale;
    }
    
    public void setBasicSale(Integer basicSale) {
        this.basicSale = basicSale;
    }
    
    public BigDecimal getBasicPriceU() {
        return basicPriceU;
    }
    
    public void setBasicPriceU(BigDecimal basicPriceU) {
        this.basicPriceU = basicPriceU;
    }
    
    public Integer getTotalQuantity() {
        return totalQuantity;
    }
    
    public void setTotalQuantity(Integer totalQuantity) {
        this.totalQuantity = totalQuantity;
    }
    
    public Integer getQuantityNotInOrders() {
        return quantityNotInOrders;
    }
    
    public void setQuantityNotInOrders(Integer quantityNotInOrders) {
        this.quantityNotInOrders = quantityNotInOrders;
    }
    
    public Integer getQuantityFull() {
        return quantityFull;
    }
    
    public void setQuantityFull(Integer quantityFull) {
        this.quantityFull = quantityFull;
    }
    
    public Integer getInWayToClient() {
        return inWayToClient;
    }
    
    public void setInWayToClient(Integer inWayToClient) {
        this.inWayToClient = inWayToClient;
    }
    
    public Integer getInWayFromClient() {
        return inWayFromClient;
    }
    
    public void setInWayFromClient(Integer inWayFromClient) {
        this.inWayFromClient = inWayFromClient;
    }
    
    public String getCategory() {
        return category;
    }
    
    public void setCategory(String category) {
        this.category = category;
    }
    
    public String getSubject() {
        return subject;
    }
    
    public void setSubject(String subject) {
        this.subject = subject;
    }
    
    public String getBrand() {
        return brand;
    }
    
    public void setBrand(String brand) {
        this.brand = brand;
    }
    
    public String getColors() {
        return colors;
    }
    
    public void setColors(String colors) {
        this.colors = colors;
    }
    
    public String getSizes() {
        return sizes;
    }
    
    public void setSizes(String sizes) {
        this.sizes = sizes;
    }
    
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
    
    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
    
    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
    
    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    @PrePersist
    public void prePersist() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = this.createdAt;
    }

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}



