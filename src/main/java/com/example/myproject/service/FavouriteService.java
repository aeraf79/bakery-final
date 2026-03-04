package com.example.myproject.service;

import com.example.myproject.dto.FavouriteDTO;
import com.example.myproject.entity.FavouriteEntity;
import com.example.myproject.entity.ProductEntity;
import com.example.myproject.entity.UserEntity;
import com.example.myproject.exception.ResourceNotFoundException;
import com.example.myproject.repository.FavouriteRepository;
import com.example.myproject.repository.ProductRepository;
import com.example.myproject.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class FavouriteService {

    @Autowired
    private FavouriteRepository favouriteRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ProductRepository productRepository;

    // ─── Helper: get user by email ─────────────────────────────────────────
    private UserEntity getUser(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + email));
    }

    // ─── Helper: map entity → DTO ──────────────────────────────────────────
    private FavouriteDTO toDTO(FavouriteEntity fav) {
        FavouriteDTO dto = new FavouriteDTO();
        dto.setFavouriteId(fav.getFavouriteId());
        dto.setProductId(fav.getProduct().getProductId());
        dto.setProductName(fav.getProduct().getName());
        dto.setProductCategory(fav.getProduct().getCategory());
        dto.setProductDescription(fav.getProduct().getDescription());
        dto.setImageUrl(fav.getProduct().getImageUrl());
        dto.setPrice(fav.getProduct().getPrice());
        dto.setUnit(fav.getProduct().getUnit());
        dto.setIsAvailable(fav.getProduct().getIsAvailable());
        dto.setStockQuantity(fav.getProduct().getStockQuantity());
        dto.setAddedAt(fav.getAddedAt());
        return dto;
    }

    // ─── GET ALL FAVOURITES ────────────────────────────────────────────────
    @Transactional  // ← FIX: keeps session open so fav.getProduct() works
    public List<FavouriteDTO> getFavourites(String email) {
        UserEntity user = getUser(email);
        return favouriteRepository.findByUser(user)
                .stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    // ─── TOGGLE FAVOURITE (add if not exists, remove if exists) ───────────
    @Transactional
    public Map<String, Object> toggleFavourite(String email, Long productId) {
        UserEntity user = getUser(email);
        ProductEntity product = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found: " + productId));

        boolean alreadyFavourited = favouriteRepository.existsByUserAndProduct(user, product);

        if (alreadyFavourited) {
            favouriteRepository.deleteByUserAndProduct(user, product);
            return Map.of(
                "isFavourited", false,
                "message", product.getName() + " removed from favourites",
                "productId", productId
            );
        } else {
            FavouriteEntity fav = new FavouriteEntity();
            fav.setUser(user);
            fav.setProduct(product);
            favouriteRepository.save(fav);
            return Map.of(
                "isFavourited", true,
                "message", product.getName() + " added to favourites",
                "productId", productId
            );
        }
    }

    // ─── CHECK IF A PRODUCT IS FAVOURITED ─────────────────────────────────
    @Transactional
    public boolean isFavourited(String email, Long productId) {
        UserEntity user = getUser(email);
        ProductEntity product = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found: " + productId));
        return favouriteRepository.existsByUserAndProduct(user, product);
    }

    // ─── GET FAVOURITE COUNT ───────────────────────────────────────────────
    @Transactional
    public Map<String, Long> getFavouriteCount(String email) {
        UserEntity user = getUser(email);
        return Map.of("count", favouriteRepository.countByUser(user));
    }

    // ─── REMOVE A SPECIFIC FAVOURITE BY ID ────────────────────────────────
    @Transactional
    public void removeFavourite(String email, Long favouriteId) {
        UserEntity user = getUser(email);
        FavouriteEntity fav = favouriteRepository.findById(favouriteId)
                .orElseThrow(() -> new ResourceNotFoundException("Favourite not found: " + favouriteId));

        if (!fav.getUser().getUserId().equals(user.getUserId())) {
            throw new RuntimeException("This favourite does not belong to your account.");
        }

        favouriteRepository.delete(fav);
    }

    // ─── CLEAR ALL FAVOURITES ──────────────────────────────────────────────
    @Transactional
    public void clearAllFavourites(String email) {
        UserEntity user = getUser(email);
        List<FavouriteEntity> favs = favouriteRepository.findByUser(user);
        favouriteRepository.deleteAll(favs);
    }

    // ─── GET ALL FAVOURITED PRODUCT IDs (for UI highlighting) ─────────────
    @Transactional  // ← FIX: keeps session open so f.getProduct() works
    public List<Long> getFavouritedProductIds(String email) {
        UserEntity user = getUser(email);
        return favouriteRepository.findByUser(user)
                .stream()
                .map(f -> f.getProduct().getProductId())
                .collect(Collectors.toList());
    }
}
