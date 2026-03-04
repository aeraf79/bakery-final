package com.example.myproject.service;

import com.example.myproject.dto.CreateOrderRequest;
import com.example.myproject.entity.*;
import com.example.myproject.repository.OrderRepository;
import com.example.myproject.repository.ProductRepository;
import com.example.myproject.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
public class OrderService {

    private final OrderRepository orderRepository;
    private final UserRepository userRepository;
    private final CartService cartService;
    private final ProductRepository productRepository;

    @Autowired
    private EmailService emailService;

    public OrderService(
        OrderRepository orderRepository,
        UserRepository userRepository,
        CartService cartService,
        ProductRepository productRepository
    ) {
        this.orderRepository = orderRepository;
        this.userRepository = userRepository;
        this.cartService = cartService;
        this.productRepository = productRepository;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // SHARED HELPERS
    // ─────────────────────────────────────────────────────────────────────────
    private void applyShipping(OrderEntity order, CreateOrderRequest request) {
        order.setShippingName(request.getName());
        order.setShippingPhone(request.getPhone());
        order.setShippingAddress(request.getAddress());
        order.setShippingCity(request.getCity());
        order.setShippingState(request.getState());
        order.setShippingPincode(request.getPincode());
        order.setOrderNotes(request.getNotes() != null ? request.getNotes() : "");
    }

    private BigDecimal calcShippingFee(BigDecimal totalAmount) {
        return totalAmount.compareTo(new BigDecimal("50")) >= 0
            ? BigDecimal.ZERO
            : new BigDecimal("5.00");
    }

    // ─────────────────────────────────────────────────────────────────────────
    // RAZORPAY — create order from cart
    // ─────────────────────────────────────────────────────────────────────────
    @Transactional
    public OrderEntity createOrderFromCart(String userEmail, CreateOrderRequest request) {
        UserEntity user = userRepository.findByEmail(userEmail)
            .orElseThrow(() -> new RuntimeException("User not found"));

        List<CartItemEntity> cartItems = cartService.getCartItems(userEmail);
        if (cartItems.isEmpty()) throw new RuntimeException("Cart is empty");

        BigDecimal totalAmount = cartItems.stream()
            .map(item -> item.getProduct().getPrice().multiply(new BigDecimal(item.getQuantity())))
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal shippingFee = calcShippingFee(totalAmount);

        OrderEntity order = new OrderEntity();
        order.setUser(user);
        order.setTotalAmount(totalAmount);
        order.setShippingFee(shippingFee);
        order.setFinalAmount(totalAmount.add(shippingFee));
        order.setStatus(OrderEntity.OrderStatus.PENDING);
        order.setPaymentStatus(OrderEntity.PaymentStatus.PENDING);
        order.setPaymentMethod(OrderEntity.PaymentMethod.RAZORPAY);
        applyShipping(order, request);

        List<OrderItemEntity> orderItems = new ArrayList<>();
        for (CartItemEntity cartItem : cartItems) {
            OrderItemEntity oi = new OrderItemEntity();
            oi.setOrder(order);
            oi.setProduct(cartItem.getProduct());
            oi.setProductName(cartItem.getProduct().getName());
            oi.setQuantity(cartItem.getQuantity());
            oi.setPriceAtPurchase(cartItem.getProduct().getPrice());
            oi.setSubtotal(cartItem.getProduct().getPrice().multiply(new BigDecimal(cartItem.getQuantity())));
            oi.setProductImageUrl(cartItem.getProduct().getImageUrl());
            orderItems.add(oi);
        }
        order.setOrderItems(orderItems);

        OrderEntity saved = orderRepository.save(order);
        cartService.clearCart(userEmail);
        emailService.sendOrderConfirmationEmail(saved);
        return saved;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // RAZORPAY — create order from Buy Now
    // ─────────────────────────────────────────────────────────────────────────
    @Transactional
    public OrderEntity createOrderFromBuyNow(String userEmail, Long productId, int quantity, CreateOrderRequest request) {
        UserEntity user = userRepository.findByEmail(userEmail)
            .orElseThrow(() -> new RuntimeException("User not found"));

        ProductEntity product = productRepository.findById(productId)
            .orElseThrow(() -> new RuntimeException("Product not found"));

        if (!product.getIsAvailable()) throw new RuntimeException("Product is not available");
        if (product.getStockQuantity() < quantity)
            throw new RuntimeException("Insufficient stock. Only " + product.getStockQuantity() + " items available.");

        BigDecimal unitPrice   = product.getPrice();
        BigDecimal totalAmount = unitPrice.multiply(new BigDecimal(quantity));
        BigDecimal shippingFee = calcShippingFee(totalAmount);

        OrderEntity order = new OrderEntity();
        order.setUser(user);
        order.setTotalAmount(totalAmount);
        order.setShippingFee(shippingFee);
        order.setFinalAmount(totalAmount.add(shippingFee));
        order.setStatus(OrderEntity.OrderStatus.PENDING);
        order.setPaymentStatus(OrderEntity.PaymentStatus.PENDING);
        order.setPaymentMethod(OrderEntity.PaymentMethod.RAZORPAY);
        applyShipping(order, request);

        OrderItemEntity oi = new OrderItemEntity();
        oi.setOrder(order);
        oi.setProduct(product);
        oi.setProductName(product.getName());
        oi.setQuantity(quantity);
        oi.setPriceAtPurchase(unitPrice);
        oi.setSubtotal(totalAmount);
        oi.setProductImageUrl(product.getImageUrl());

        List<OrderItemEntity> items = new ArrayList<>();
        items.add(oi);
        order.setOrderItems(items);
        return orderRepository.save(order);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // COD — create order from cart
    // ─────────────────────────────────────────────────────────────────────────
    @Transactional
    public OrderEntity createCodOrderFromCart(String userEmail, CreateOrderRequest request) {
        UserEntity user = userRepository.findByEmail(userEmail)
            .orElseThrow(() -> new RuntimeException("User not found"));

        List<CartItemEntity> cartItems = cartService.getCartItems(userEmail);
        if (cartItems.isEmpty()) throw new RuntimeException("Cart is empty");

        BigDecimal totalAmount = cartItems.stream()
            .map(item -> item.getProduct().getPrice().multiply(new BigDecimal(item.getQuantity())))
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal shippingFee = calcShippingFee(totalAmount);

        OrderEntity order = new OrderEntity();
        order.setUser(user);
        order.setTotalAmount(totalAmount);
        order.setShippingFee(shippingFee);
        order.setFinalAmount(totalAmount.add(shippingFee));
        order.setStatus(OrderEntity.OrderStatus.CONFIRMED);
        order.setPaymentStatus(OrderEntity.PaymentStatus.PENDING);
        order.setPaymentMethod(OrderEntity.PaymentMethod.COD);
        applyShipping(order, request);

        List<OrderItemEntity> orderItems = new ArrayList<>();
        for (CartItemEntity cartItem : cartItems) {
            OrderItemEntity oi = new OrderItemEntity();
            oi.setOrder(order);
            oi.setProduct(cartItem.getProduct());
            oi.setProductName(cartItem.getProduct().getName());
            oi.setQuantity(cartItem.getQuantity());
            oi.setPriceAtPurchase(cartItem.getProduct().getPrice());
            oi.setSubtotal(cartItem.getProduct().getPrice().multiply(new BigDecimal(cartItem.getQuantity())));
            oi.setProductImageUrl(cartItem.getProduct().getImageUrl());
            orderItems.add(oi);
        }
        order.setOrderItems(orderItems);

        OrderEntity saved = orderRepository.save(order);
        cartService.clearCart(userEmail);
        emailService.sendOrderConfirmationEmail(saved);
        return saved;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // COD — create order from Buy Now
    // ─────────────────────────────────────────────────────────────────────────
    @Transactional
    public OrderEntity createCodOrderFromBuyNow(String userEmail, Long productId, int quantity, CreateOrderRequest request) {
        UserEntity user = userRepository.findByEmail(userEmail)
            .orElseThrow(() -> new RuntimeException("User not found"));

        ProductEntity product = productRepository.findById(productId)
            .orElseThrow(() -> new RuntimeException("Product not found"));

        if (!product.getIsAvailable()) throw new RuntimeException("Product is not available");
        if (product.getStockQuantity() < quantity)
            throw new RuntimeException("Insufficient stock. Only " + product.getStockQuantity() + " items available.");

        BigDecimal unitPrice   = product.getPrice();
        BigDecimal totalAmount = unitPrice.multiply(new BigDecimal(quantity));
        BigDecimal shippingFee = calcShippingFee(totalAmount);

        OrderEntity order = new OrderEntity();
        order.setUser(user);
        order.setTotalAmount(totalAmount);
        order.setShippingFee(shippingFee);
        order.setFinalAmount(totalAmount.add(shippingFee));
        order.setStatus(OrderEntity.OrderStatus.CONFIRMED);
        order.setPaymentStatus(OrderEntity.PaymentStatus.PENDING);
        order.setPaymentMethod(OrderEntity.PaymentMethod.COD);
        applyShipping(order, request);

        OrderItemEntity oi = new OrderItemEntity();
        oi.setOrder(order);
        oi.setProduct(product);
        oi.setProductName(product.getName());
        oi.setQuantity(quantity);
        oi.setPriceAtPurchase(unitPrice);
        oi.setSubtotal(totalAmount);
        oi.setProductImageUrl(product.getImageUrl());

        List<OrderItemEntity> items = new ArrayList<>();
        items.add(oi);
        order.setOrderItems(items);
        OrderEntity saved = orderRepository.save(order);
        emailService.sendOrderConfirmationEmail(saved);
        return saved;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // QUERY METHODS — all need @Transactional so lazy relations load
    // ─────────────────────────────────────────────────────────────────────────

    /** All orders — admin only */
    @Transactional(readOnly = true)  // ← FIX: keeps session open for OrderDTO.fromEntity()
    public List<OrderEntity> getAllOrders() {
        return orderRepository.findAllByOrderByCreatedAtDesc();
    }

    /** A specific user's orders */
    @Transactional(readOnly = true)  // ← FIX: keeps session open for OrderDTO.fromEntity()
    public List<OrderEntity> getUserOrders(String userEmail) {
        UserEntity user = userRepository.findByEmail(userEmail)
            .orElseThrow(() -> new RuntimeException("User not found"));
        return orderRepository.findByUserOrderByCreatedAtDesc(user);
    }

    /** Single order with ownership check (for regular users) */
    @Transactional(readOnly = true)  // ← FIX
    public OrderEntity getOrderById(String userEmail, Long orderId) {
        UserEntity user = userRepository.findByEmail(userEmail)
            .orElseThrow(() -> new RuntimeException("User not found"));
        OrderEntity order = orderRepository.findById(orderId)
            .orElseThrow(() -> new RuntimeException("Order not found"));
        if (!order.getUser().getUserId().equals(user.getUserId()))
            throw new RuntimeException("Unauthorized access to order");
        return order;
    }

    /** Single order without ownership check (admin only) */
    @Transactional(readOnly = true)  // ← FIX
    public OrderEntity getOrderByIdAdmin(Long orderId) {
        return orderRepository.findById(orderId)
            .orElseThrow(() -> new RuntimeException("Order not found"));
    }

    /** Find by order number */
    @Transactional(readOnly = true)  // ← FIX
    public OrderEntity getOrderByOrderNumber(String orderNumber) {
        return orderRepository.findByOrderNumber(orderNumber)
            .orElseThrow(() -> new RuntimeException("Order not found"));
    }

    /** Cancel order — only if not already paid */
    @Transactional
    public OrderEntity cancelOrder(String userEmail, Long orderId) {
        OrderEntity order = getOrderById(userEmail, orderId);
        if (order.getPaymentStatus() == OrderEntity.PaymentStatus.PAID)
            throw new RuntimeException("Cannot cancel paid order. Please contact support for refund.");
        order.setStatus(OrderEntity.OrderStatus.CANCELLED);
        return orderRepository.save(order);
    }

    /** Admin: update status + payment status with auto-timestamps */
    @Transactional
    public OrderEntity updateOrderStatus(Long orderId, String status, String paymentStatus) {
        OrderEntity order = orderRepository.findById(orderId)
            .orElseThrow(() -> new RuntimeException("Order not found"));

        if (status != null) {
            order.setStatus(OrderEntity.OrderStatus.valueOf(status));
            if ("DELIVERED".equals(status) && order.getDeliveredAt() == null)
                order.setDeliveredAt(LocalDateTime.now());
        }
        if (paymentStatus != null) {
            order.setPaymentStatus(OrderEntity.PaymentStatus.valueOf(paymentStatus));
            if ("PAID".equals(paymentStatus) && order.getPaidAt() == null)
                order.setPaidAt(LocalDateTime.now());
        }

        return orderRepository.save(order);
    }
}
