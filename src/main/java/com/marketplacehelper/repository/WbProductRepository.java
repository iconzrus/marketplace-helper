package com.marketplacehelper.repository;

import com.marketplacehelper.model.WbProduct;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface WbProductRepository extends JpaRepository<WbProduct, Long> {
    
    Optional<WbProduct> findByNmId(Long nmId);
    
    List<WbProduct> findByVendor(String vendor);
    
    List<WbProduct> findByBrand(String brand);
    
    List<WbProduct> findByCategory(String category);
    
    List<WbProduct> findBySubject(String subject);
    
    @Query("SELECT w FROM WbProduct w WHERE w.name LIKE %:name%")
    List<WbProduct> findByNameContaining(@Param("name") String name);
    
    @Query("SELECT w FROM WbProduct w WHERE w.totalQuantity < :threshold")
    List<WbProduct> findLowStockProducts(@Param("threshold") Integer threshold);
    
    @Query("SELECT w FROM WbProduct w WHERE w.price BETWEEN :minPrice AND :maxPrice")
    List<WbProduct> findByPriceRange(@Param("minPrice") java.math.BigDecimal minPrice, 
                                    @Param("maxPrice") java.math.BigDecimal maxPrice);
    
    @Query("SELECT w FROM WbProduct w WHERE w.discount > :minDiscount")
    List<WbProduct> findByDiscountGreaterThan(@Param("minDiscount") Integer minDiscount);
}



