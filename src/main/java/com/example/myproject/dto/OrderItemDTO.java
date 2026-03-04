package com.example.myproject.dto;

import com.example.myproject.entity.OrderItemEntity;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrderItemDTO {
    
    private Long orderItemId;
    private Long productId;
    private String productName;
    private Integer quantity;
    private BigDecimal priceAtPurchase;
    private BigDecimal subtotal;
    private String productImageUrl;

    /**
     * Convert OrderItemEntity to OrderItemDTO
     */
    public static OrderItemDTO fromEntity(OrderItemEntity orderItem) {
        OrderItemDTO dto = new OrderItemDTO();
        dto.setOrderItemId(orderItem.getOrderItemId());
        dto.setProductId(orderItem.getProduct().getProductId());
        dto.setProductName(orderItem.getProductName());
        dto.setQuantity(orderItem.getQuantity());
        dto.setPriceAtPurchase(orderItem.getPriceAtPurchase());
        dto.setSubtotal(orderItem.getSubtotal());
        dto.setProductImageUrl(orderItem.getProductImageUrl());
        return dto;
    }
}