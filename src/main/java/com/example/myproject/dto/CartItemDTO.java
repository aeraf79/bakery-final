package com.example.myproject.dto;

import java.math.BigDecimal;

// ✅ Matches EXACTLY what Cart.jsx reads:
// item.cartItemId, item.productName, item.productCategory,
// item.productDescription, item.imageUrl, item.price,
// item.subtotal, item.quantity, item.stockQuantity, item.isAvailable
public class CartItemDTO {

    private Long cartItemId;
    private Long productId;
    private String productName;
    private String productCategory;
    private String productDescription;
    private String imageUrl;
    private BigDecimal price;
    private BigDecimal subtotal;
    private Integer quantity;
    private Integer stockQuantity;
    private Boolean isAvailable;

    // Getters & Setters
    public Long getCartItemId() { return cartItemId; }
    public void setCartItemId(Long cartItemId) { this.cartItemId = cartItemId; }
    public Long getProductId() { return productId; }
    public void setProductId(Long productId) { this.productId = productId; }
    public String getProductName() { return productName; }
    public void setProductName(String productName) { this.productName = productName; }
    public String getProductCategory() { return productCategory; }
    public void setProductCategory(String productCategory) { this.productCategory = productCategory; }
    public String getProductDescription() { return productDescription; }
    public void setProductDescription(String productDescription) { this.productDescription = productDescription; }
    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }
    public BigDecimal getPrice() { return price; }
    public void setPrice(BigDecimal price) { this.price = price; }
    public BigDecimal getSubtotal() { return subtotal; }
    public void setSubtotal(BigDecimal subtotal) { this.subtotal = subtotal; }
    public Integer getQuantity() { return quantity; }
    public void setQuantity(Integer quantity) { this.quantity = quantity; }
    public Integer getStockQuantity() { return stockQuantity; }
    public void setStockQuantity(Integer stockQuantity) { this.stockQuantity = stockQuantity; }
    public Boolean getIsAvailable() { return isAvailable; }
    public void setIsAvailable(Boolean isAvailable) { this.isAvailable = isAvailable; }
}