package com.example.myproject.repository;

import com.example.myproject.entity.CartEntity;
import com.example.myproject.entity.UserEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CartRepository extends JpaRepository<CartEntity, Long> {
    Optional<CartEntity> findByUser(UserEntity user);
    Optional<CartEntity> findByUserEmail(String email);
}