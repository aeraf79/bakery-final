package com.example.myproject.service;

import com.example.myproject.dto.RazorpayOrderResponse;
import com.example.myproject.entity.OrderEntity;
import com.example.myproject.repository.OrderRepository;
import com.razorpay.Order;
import com.razorpay.RazorpayClient;
import com.razorpay.RazorpayException;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Service
public class RazorpayService {

    @Value("${razorpay.key.id}")
    private String razorpayKeyId;

    @Value("${razorpay.key.secret}")
    private String razorpayKeySecret;

    private final OrderRepository orderRepository;

    @Autowired
    private EmailService emailService;

    public RazorpayService(OrderRepository orderRepository) {
        this.orderRepository = orderRepository;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // CREATE RAZORPAY ORDER
    // Called AFTER our OrderEntity is already saved (PENDING state).
    // We re-fetch the order by ID inside @Transactional so the session is fresh
    // and lazy relations (user, orderItems) are fully accessible.
    // ─────────────────────────────────────────────────────────────────────────
    @Transactional
    public RazorpayOrderResponse createRazorpayOrder(OrderEntity orderArg) throws RazorpayException {
        try {
            // Re-fetch by ID to ensure we have a managed entity with all relations loaded.
            // This avoids LazyInitializationException regardless of how the caller's
            // transaction was scoped (critical for the Buy Now → Razorpay path).
            Long orderId = orderArg.getOrderId();
            OrderEntity order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found with id: " + orderId));

            // Amount in paise (Razorpay requires integer paise)
            int amountInPaise = order.getFinalAmount()
                .multiply(new BigDecimal("100"))
                .intValue();

            JSONObject orderRequest = new JSONObject();
            orderRequest.put("amount", amountInPaise);
            orderRequest.put("currency", "INR");
            orderRequest.put("receipt", order.getOrderNumber());

            JSONObject notes = new JSONObject();
            notes.put("order_number", order.getOrderNumber());
            notes.put("customer_name", order.getShippingName());
            notes.put("customer_phone", order.getShippingPhone());
            orderRequest.put("notes", notes);

            // Call Razorpay API
            RazorpayClient razorpayClient = new RazorpayClient(razorpayKeyId, razorpayKeySecret);
            Order razorpayOrder = razorpayClient.orders.create(orderRequest);
            String razorpayOrderId = razorpayOrder.get("id");

            // Persist the Razorpay order ID back to our DB
            order.setRazorpayOrderId(razorpayOrderId);
            order.setPaymentMethod(OrderEntity.PaymentMethod.RAZORPAY);
            orderRepository.save(order);

            // Build response for frontend
            RazorpayOrderResponse response = new RazorpayOrderResponse();
            response.setRazorpay_order_id(razorpayOrderId);
            response.setRazorpay_key_id(razorpayKeyId);
            response.setAmount(amountInPaise);
            response.setCurrency("INR");
            response.setOrder_number(order.getOrderNumber());
            response.setOrder_id(order.getOrderId());
            response.setFinal_amount(order.getFinalAmount());
            response.setCustomer_name(order.getShippingName());
            response.setCustomer_email(order.getUser().getEmail());  // safe: managed entity
            response.setCustomer_phone(order.getShippingPhone());

            System.out.println("✅ Razorpay order created: " + razorpayOrderId
                + " for order: " + order.getOrderNumber());
            return response;

        } catch (RazorpayException e) {
            System.err.println("❌ Razorpay API error: " + e.getMessage());
            throw new RazorpayException("Failed to create Razorpay order: " + e.getMessage());
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // VERIFY PAYMENT SIGNATURE
    // ─────────────────────────────────────────────────────────────────────────
    public boolean verifyPaymentSignature(
        String razorpayOrderId,
        String razorpayPaymentId,
        String razorpaySignature
    ) {
        try {
            String payload = razorpayOrderId + "|" + razorpayPaymentId;
            String expectedSignature = generateHmacSha256(payload, razorpayKeySecret);
            return expectedSignature.equals(razorpaySignature);
        } catch (Exception e) {
            System.err.println("❌ Signature verification error: " + e.getMessage());
            return false;
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // UPDATE ORDER AFTER SUCCESSFUL PAYMENT
    // ─────────────────────────────────────────────────────────────────────────
    @Transactional
    public OrderEntity updateOrderAfterPayment(
        String razorpayOrderId,
        String razorpayPaymentId,
        String razorpaySignature
    ) {
        OrderEntity order = orderRepository.findByRazorpayOrderId(razorpayOrderId)
            .orElseThrow(() -> new RuntimeException("Order not found for razorpay_order_id: " + razorpayOrderId));

        order.setRazorpayPaymentId(razorpayPaymentId);
        order.setRazorpaySignature(razorpaySignature);
        order.setPaymentStatus(OrderEntity.PaymentStatus.PAID);
        order.setStatus(OrderEntity.OrderStatus.CONFIRMED);
        order.setPaidAt(LocalDateTime.now());

        OrderEntity paidOrder = orderRepository.save(order);

        // Send order confirmation email (async — does not block response)
        emailService.sendOrderConfirmationEmail(paidOrder);

        return paidOrder;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // HANDLE PAYMENT FAILURE
    // ─────────────────────────────────────────────────────────────────────────
    @Transactional
    public OrderEntity handlePaymentFailure(String razorpayOrderId, String reason) {
        OrderEntity order = orderRepository.findByRazorpayOrderId(razorpayOrderId)
            .orElseThrow(() -> new RuntimeException("Order not found for razorpay_order_id: " + razorpayOrderId));

        order.setPaymentStatus(OrderEntity.PaymentStatus.FAILED);
        order.setOrderNotes("Payment failed: " + reason);

        return orderRepository.save(order);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // GET KEY ID (for /api/payment/config endpoint)
    // ─────────────────────────────────────────────────────────────────────────
    public String getRazorpayKeyId() {
        return razorpayKeyId;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // HMAC-SHA256 HELPER
    // ─────────────────────────────────────────────────────────────────────────
    private String generateHmacSha256(String payload, String secret) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(secret.getBytes(), "HmacSHA256"));
        byte[] hash = mac.doFinal(payload.getBytes());
        StringBuilder hex = new StringBuilder();
        for (byte b : hash) {
            String h = Integer.toHexString(0xff & b);
            if (h.length() == 1) hex.append('0');
            hex.append(h);
        }
        return hex.toString();
    }
}