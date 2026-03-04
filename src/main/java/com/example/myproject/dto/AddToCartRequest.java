package com.example.myproject.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

// ✅ Matches UserPanel.jsx: { productId: product.productId, quantity: 1 }
public class AddToCartRequest {

    @NotNull(message = "Product ID is required")
    private Long productId;

    @Min(value = 1, message = "Quantity must be at least 1")
    private Integer quantity = 1;

    public Long getProductId() { return productId; }
    public void setProductId(Long productId) { this.productId = productId; }
    public Integer getQuantity() { return quantity; }
    public void setQuantity(Integer quantity) { this.quantity = quantity; }
}