package com.marketplacehelper.service;

import com.marketplacehelper.model.Product;
import com.marketplacehelper.repository.ProductRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class ProductService {
    
    private final ProductRepository productRepository;
    
    @Autowired
    public ProductService(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }
    
    public List<Product> getAllProducts() {
        return productRepository.findAll();
    }
    
    public Optional<Product> getProductById(Long id) {
        return productRepository.findById(id);
    }
    
    public Optional<Product> getProductByWbArticle(String wbArticle) {
        return productRepository.findByWbArticle(wbArticle);
    }
    
    public Product saveProduct(Product product) {
        return productRepository.save(product);
    }
    
    public Product updateProduct(Long id, Product productDetails) {
        return productRepository.findById(id)
                .map(product -> {
                    product.setName(productDetails.getName());
                    product.setWbArticle(productDetails.getWbArticle());
                    product.setWbBarcode(productDetails.getWbBarcode());
                    product.setPrice(productDetails.getPrice());
                    product.setStockQuantity(productDetails.getStockQuantity());
                    product.setCategory(productDetails.getCategory());
                    product.setBrand(productDetails.getBrand());
                    product.setPurchasePrice(productDetails.getPurchasePrice());
                    product.setLogisticsCost(productDetails.getLogisticsCost());
                    product.setMarketingCost(productDetails.getMarketingCost());
                    product.setOtherExpenses(productDetails.getOtherExpenses());
                    return productRepository.save(product);
                })
                .orElseThrow(() -> new RuntimeException("Товар с ID " + id + " не найден"));
    }

    public Product updateProductCosts(Long id, java.math.BigDecimal purchase,
                                      java.math.BigDecimal logistics,
                                      java.math.BigDecimal marketing,
                                      java.math.BigDecimal other) {
        return productRepository.findById(id)
                .map(product -> {
                    if (purchase != null) product.setPurchasePrice(purchase);
                    if (logistics != null) product.setLogisticsCost(logistics);
                    if (marketing != null) product.setMarketingCost(marketing);
                    if (other != null) product.setOtherExpenses(other);
                    return productRepository.save(product);
                })
                .orElseThrow(() -> new RuntimeException("Товар с ID " + id + " не найден"));
    }
    
    public void deleteProduct(Long id) {
        productRepository.deleteById(id);
    }
    
    public List<Product> getProductsByCategory(String category) {
        return productRepository.findByCategory(category);
    }
    
    public List<Product> getProductsByBrand(String brand) {
        return productRepository.findByBrand(brand);
    }
    
    public List<Product> searchProductsByName(String name) {
        return productRepository.findByNameContaining(name);
    }
    
    public List<Product> getLowStockProducts(Integer threshold) {
        return productRepository.findLowStockProducts(threshold);
    }

    public Product updateProductPrice(Long id, java.math.BigDecimal newPrice) {
        return productRepository.findById(id)
                .map(product -> {
                    if (newPrice != null) {
                        product.setPrice(newPrice);
                    }
                    return productRepository.save(product);
                })
                .orElseThrow(() -> new RuntimeException("Товар с ID " + id + " не найден"));
    }
}



