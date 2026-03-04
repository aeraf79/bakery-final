package com.example.myproject.repository;

import com.example.myproject.entity.FavouriteEntity;
import com.example.myproject.entity.ProductEntity;
import com.example.myproject.entity.UserEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FavouriteRepository extends JpaRepository<FavouriteEntity, Long> {

    // Get all favourites for a user
    List<FavouriteEntity> findByUser(UserEntity user);

    // Check if a specific product is already favourited by a user
    Optional<FavouriteEntity> findByUserAndProduct(UserEntity user, ProductEntity product);

    // Check existence (used for toggle)
    boolean existsByUserAndProduct(UserEntity user, ProductEntity product);

    // Delete by user + product (for toggle remove)
    void deleteByUserAndProduct(UserEntity user, ProductEntity product);

    // Count favourites for a user
    long countByUser(UserEntity user);
}