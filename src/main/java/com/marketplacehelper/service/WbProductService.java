package com.marketplacehelper.service;

import com.marketplacehelper.model.WbProduct;
import com.marketplacehelper.repository.WbProductRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class WbProductService {
    
    private final WbProductRepository wbProductRepository;
    
    @Autowired
    public WbProductService(WbProductRepository wbProductRepository) {
        this.wbProductRepository = wbProductRepository;
    }
    
    public List<WbProduct> getAllWbProducts() {
        return wbProductRepository.findAll();
    }
    
    public Optional<WbProduct> getWbProductById(Long id) {
        return wbProductRepository.findById(id);
    }
    
    public Optional<WbProduct> getWbProductByNmId(Long nmId) {
        return wbProductRepository.findByNmId(nmId);
    }
    
    public WbProduct saveWbProduct(WbProduct wbProduct) {
        return wbProductRepository.save(wbProduct);
    }
    
    public WbProduct updateWbProduct(Long id, WbProduct wbProductDetails) {
        return wbProductRepository.findById(id)
                .map(wbProduct -> {
                    wbProduct.setNmId(wbProductDetails.getNmId());
                    wbProduct.setName(wbProductDetails.getName());
                    wbProduct.setVendor(wbProductDetails.getVendor());
                    wbProduct.setVendorCode(wbProductDetails.getVendorCode());
                    wbProduct.setPrice(wbProductDetails.getPrice());
                    wbProduct.setDiscount(wbProductDetails.getDiscount());
                    wbProduct.setPriceWithDiscount(wbProductDetails.getPriceWithDiscount());
                    wbProduct.setSalePrice(wbProductDetails.getSalePrice());
                    wbProduct.setSale(wbProductDetails.getSale());
                    wbProduct.setBasicSale(wbProductDetails.getBasicSale());
                    wbProduct.setBasicPriceU(wbProductDetails.getBasicPriceU());
                    wbProduct.setTotalQuantity(wbProductDetails.getTotalQuantity());
                    wbProduct.setQuantityNotInOrders(wbProductDetails.getQuantityNotInOrders());
                    wbProduct.setQuantityFull(wbProductDetails.getQuantityFull());
                    wbProduct.setInWayToClient(wbProductDetails.getInWayToClient());
                    wbProduct.setInWayFromClient(wbProductDetails.getInWayFromClient());
                    wbProduct.setCategory(wbProductDetails.getCategory());
                    wbProduct.setSubject(wbProductDetails.getSubject());
                    wbProduct.setBrand(wbProductDetails.getBrand());
                    wbProduct.setColors(wbProductDetails.getColors());
                    wbProduct.setSizes(wbProductDetails.getSizes());
                    return wbProductRepository.save(wbProduct);
                })
                .orElseThrow(() -> new RuntimeException("Товар WB с ID " + id + " не найден"));
    }
    
    public void deleteWbProduct(Long id) {
        wbProductRepository.deleteById(id);
    }
    
    public List<WbProduct> getWbProductsByVendor(String vendor) {
        return wbProductRepository.findByVendor(vendor);
    }
    
    public List<WbProduct> getWbProductsByBrand(String brand) {
        return wbProductRepository.findByBrand(brand);
    }
    
    public List<WbProduct> getWbProductsByCategory(String category) {
        return wbProductRepository.findByCategory(category);
    }
    
    public List<WbProduct> getWbProductsBySubject(String subject) {
        return wbProductRepository.findBySubject(subject);
    }
    
    public List<WbProduct> searchWbProductsByName(String name) {
        return wbProductRepository.findByNameContaining(name);
    }
    
    public List<WbProduct> getLowStockWbProducts(Integer threshold) {
        return wbProductRepository.findLowStockProducts(threshold);
    }
    
    public List<WbProduct> getWbProductsByPriceRange(BigDecimal minPrice, BigDecimal maxPrice) {
        return wbProductRepository.findByPriceRange(minPrice, maxPrice);
    }
    
    public List<WbProduct> getWbProductsByDiscount(Integer minDiscount) {
        return wbProductRepository.findByDiscountGreaterThan(minDiscount);
    }
}



