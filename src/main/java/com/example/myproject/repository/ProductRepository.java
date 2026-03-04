package com.example.myproject.repository;

import com.example.myproject.entity.ProductEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;

@Repository
public interface ProductRepository extends JpaRepository<ProductEntity, Long> {

    List<ProductEntity> findByCategory(String category);

    List<ProductEntity> findByIsAvailableTrue();

    @Query("SELECT p FROM ProductEntity p WHERE p.stockQuantity <= :threshold")
    List<ProductEntity> findLowStockProducts(@Param("threshold") Integer threshold);

    @Query("SELECT p FROM ProductEntity p WHERE LOWER(p.name) LIKE LOWER(CONCAT('%', :query, '%')) " +
           "OR LOWER(p.description) LIKE LOWER(CONCAT('%', :query, '%'))")
    List<ProductEntity> searchProducts(@Param("query") String query);

    List<ProductEntity> findByPriceBetween(BigDecimal minPrice, BigDecimal maxPrice);
}