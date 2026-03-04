package com.example.myproject.repository;

import com.example.myproject.entity.OrderEntity;
import com.example.myproject.entity.UserEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface OrderRepository extends JpaRepository<OrderEntity, Long> {

    // ✅ NEW: Fetch all orders sorted by newest first (for admin)
    List<OrderEntity> findAllByOrderByCreatedAtDesc();

    List<OrderEntity> findByUserOrderByCreatedAtDesc(UserEntity user);

    Optional<OrderEntity> findByOrderNumber(String orderNumber);

    Optional<OrderEntity> findByRazorpayOrderId(String razorpayOrderId);

    List<OrderEntity> findByUserAndStatusOrderByCreatedAtDesc(
        UserEntity user,
        OrderEntity.OrderStatus status
    );

    List<OrderEntity> findByUserAndPaymentStatusOrderByCreatedAtDesc(
        UserEntity user,
        OrderEntity.PaymentStatus paymentStatus
    );
}