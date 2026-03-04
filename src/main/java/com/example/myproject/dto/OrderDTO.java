package com.example.myproject.dto;

import com.example.myproject.entity.OrderEntity;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrderDTO {
    
    private Long orderId;
    private String orderNumber;
    private BigDecimal totalAmount;
    private BigDecimal discountAmount;
    private BigDecimal shippingFee;
    private BigDecimal finalAmount;
    private String status;
    private String paymentStatus;
    private String paymentMethod;
    
    // Shipping details
    private String shippingName;
    private String shippingPhone;
    private String shippingAddress;
    private String shippingCity;
    private String shippingState;
    private String shippingPincode;
    private String orderNotes;
    
    // Order items
    private List<OrderItemDTO> orderItems;
    
    // Timestamps
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime paidAt;
    private LocalDateTime deliveredAt;
    
    // Customer info (optional, for admin)
    private String customerEmail;
    private String customerName;

    /**
     * Convert OrderEntity to OrderDTO
     */
    public static OrderDTO fromEntity(OrderEntity order) {
        OrderDTO dto = new OrderDTO();
        dto.setOrderId(order.getOrderId());
        dto.setOrderNumber(order.getOrderNumber());
        dto.setTotalAmount(order.getTotalAmount());
        dto.setDiscountAmount(order.getDiscountAmount());
        dto.setShippingFee(order.getShippingFee());
        dto.setFinalAmount(order.getFinalAmount());
        dto.setStatus(order.getStatus().name());
        dto.setPaymentStatus(order.getPaymentStatus().name());
        dto.setPaymentMethod(order.getPaymentMethod() != null ? order.getPaymentMethod().name() : null);
        
        dto.setShippingName(order.getShippingName());
        dto.setShippingPhone(order.getShippingPhone());
        dto.setShippingAddress(order.getShippingAddress());
        dto.setShippingCity(order.getShippingCity());
        dto.setShippingState(order.getShippingState());
        dto.setShippingPincode(order.getShippingPincode());
        dto.setOrderNotes(order.getOrderNotes());
        
        dto.setCreatedAt(order.getCreatedAt());
        dto.setUpdatedAt(order.getUpdatedAt());
        dto.setPaidAt(order.getPaidAt());
        dto.setDeliveredAt(order.getDeliveredAt());
        
        // Convert order items
        if (order.getOrderItems() != null) {
            dto.setOrderItems(
                order.getOrderItems().stream()
                    .map(OrderItemDTO::fromEntity)
                    .collect(Collectors.toList())
            );
        }
        
        // Customer info
        if (order.getUser() != null) {
            dto.setCustomerEmail(order.getUser().getEmail());
            dto.setCustomerName(order.getUser().getFullName());
        }
        
        return dto;
    }
}