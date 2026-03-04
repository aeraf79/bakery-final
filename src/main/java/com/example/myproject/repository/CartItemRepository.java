package com.example.myproject.repository;

import com.example.myproject.entity.CartEntity;
import com.example.myproject.entity.CartItemEntity;
import com.example.myproject.entity.ProductEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CartItemRepository extends JpaRepository<CartItemEntity, Long> {
    Optional<CartItemEntity> findByCartAndProduct(CartEntity cart, ProductEntity product);
}