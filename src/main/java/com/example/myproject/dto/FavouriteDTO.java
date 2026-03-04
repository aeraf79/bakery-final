package com.example.myproject.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class FavouriteDTO {

    private Long favouriteId;
    private Long productId;
    private String productName;
    private String productCategory;
    private String productDescription;
    private String imageUrl;
    private BigDecimal price;
    private String unit;
    private Boolean isAvailable;
    private Integer stockQuantity;
    private LocalDateTime addedAt;

    // Getters & Setters
    public Long getFavouriteId() { return favouriteId; }
    public void setFavouriteId(Long favouriteId) { this.favouriteId = favouriteId; }

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

    public String getUnit() { return unit; }
    public void setUnit(String unit) { this.unit = unit; }

    public Boolean getIsAvailable() { return isAvailable; }
    public void setIsAvailable(Boolean isAvailable) { this.isAvailable = isAvailable; }

    public Integer getStockQuantity() { return stockQuantity; }
    public void setStockQuantity(Integer stockQuantity) { this.stockQuantity = stockQuantity; }

    public LocalDateTime getAddedAt() { return addedAt; }
    public void setAddedAt(LocalDateTime addedAt) { this.addedAt = addedAt; }
}