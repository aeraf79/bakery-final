package com.example.myproject.controller;

import com.example.myproject.dto.*;
import com.example.myproject.entity.OrderEntity;
import com.example.myproject.service.OrderService;
import com.example.myproject.service.RazorpayService;
import com.razorpay.RazorpayException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.security.Principal;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/payment")

public class PaymentController {

    private final OrderService orderService;
    private final RazorpayService razorpayService;

    public PaymentController(OrderService orderService, RazorpayService razorpayService) {
        this.orderService = orderService;
        this.razorpayService = razorpayService;
    }

    /**
     * Step 1: Create order from cart
     * POST /api/payment/create-order
     */
    @PostMapping("/create-order")
    public ResponseEntity<?> createOrder(
        @Valid @RequestBody CreateOrderRequest request,
        Principal principal
    ) {
        try {
            String userEmail = principal.getName();

            // Create order from cart
            OrderEntity order = orderService.createOrderFromCart(userEmail, request);

            // Create Razorpay order
            RazorpayOrderResponse response = razorpayService.createRazorpayOrder(order);

            return ResponseEntity.ok(response);

        } catch (RazorpayException e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "Failed to create Razorpay order");
            error.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }

    /**
     * Step 1b: Create order from Buy Now (single product, bypasses cart)
     * POST /api/payment/create-order/buy-now?productId=X&quantity=Y
     */
    @PostMapping("/create-order/buy-now")
    public ResponseEntity<?> createOrderBuyNow(
        @RequestParam Long productId,
        @RequestParam(defaultValue = "1") int quantity,
        @Valid @RequestBody CreateOrderRequest request,
        Principal principal
    ) {
        try {
            String userEmail = principal.getName();

            OrderEntity order = orderService.createOrderFromBuyNow(userEmail, productId, quantity, request);

            RazorpayOrderResponse response = razorpayService.createRazorpayOrder(order);

            return ResponseEntity.ok(response);

        } catch (RazorpayException e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "Failed to create Razorpay order");
            error.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }

    /**
     * Step 2: Verify payment after user completes payment
     * POST /api/payment/verify
     */
    @PostMapping("/verify")
    public ResponseEntity<?> verifyPayment(
        @Valid @RequestBody PaymentVerificationRequest request,
        Principal principal
    ) {
        try {
            // Verify signature
            boolean isValid = razorpayService.verifyPaymentSignature(
                request.getRazorpay_order_id(),
                request.getRazorpay_payment_id(),
                request.getRazorpay_signature()
            );

            if (!isValid) {
                razorpayService.handlePaymentFailure(request.getRazorpay_order_id(), "Invalid signature");
                return ResponseEntity.badRequest()
                    .body(PaymentResponse.failure("Payment verification failed"));
            }

            // Update order with payment details
            OrderEntity order = razorpayService.updateOrderAfterPayment(
                request.getRazorpay_order_id(),
                request.getRazorpay_payment_id(),
                request.getRazorpay_signature()
            );

            PaymentResponse response = PaymentResponse.success(
                order.getOrderNumber(),
                order.getOrderId(),
                "Payment successful"
            );
            response.setPaymentStatus(order.getPaymentStatus().name());
            response.setOrderStatus(order.getStatus().name());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            return ResponseEntity.badRequest()
                .body(PaymentResponse.failure(e.getMessage()));
        }
    }

    /**
     * Handle payment failure
     * POST /api/payment/failure
     */
    @PostMapping("/failure")
    public ResponseEntity<?> handlePaymentFailure(
        @RequestBody Map<String, String> failureData,
        Principal principal
    ) {
        try {
            String razorpayOrderId = failureData.get("razorpay_order_id");
            String reason = failureData.getOrDefault("reason", "Payment cancelled by user");

            OrderEntity order = razorpayService.handlePaymentFailure(razorpayOrderId, reason);

            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Payment failed");
            response.put("order_number", order.getOrderNumber());
            response.put("order_id", order.getOrderId());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }

    /**
     * Get Razorpay Key ID (for frontend)
     * GET /api/payment/config
     */
    @GetMapping("/config")
    public ResponseEntity<?> getPaymentConfig() {
        Map<String, String> config = new HashMap<>();
        config.put("razorpay_key_id", razorpayService.getRazorpayKeyId());
        return ResponseEntity.ok(config);
    }
}