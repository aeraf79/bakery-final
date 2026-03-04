package com.example.myproject.service;

import com.example.myproject.dto.AddToCartRequest;
import com.example.myproject.dto.CartDTO;
import com.example.myproject.dto.CartItemDTO;
import com.example.myproject.entity.*;
import com.example.myproject.exception.ResourceNotFoundException;
import com.example.myproject.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class CartService {

    @Autowired
    private CartRepository cartRepository;

    @Autowired
    private CartItemRepository cartItemRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ProductRepository productRepository;

    // ─── Helper: get or create cart for user ───────────────────────────────
    // NOTE: must be called from within a @Transactional method
    private CartEntity getOrCreateCart(String email) {
        UserEntity user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + email));

        return cartRepository.findByUser(user)
                .orElseGet(() -> {
                    CartEntity newCart = new CartEntity();
                    newCart.setUser(user);
                    newCart.setTotalAmount(BigDecimal.ZERO);
                    newCart.setTotalItems(0);
                    return cartRepository.save(newCart);
                });
    }

    // ─── Helper: map CartEntity → CartDTO ──────────────────────────────────
    private CartDTO toDTO(CartEntity cart) {
        CartDTO dto = new CartDTO();
        dto.setCartId(cart.getCartId());
        dto.setUserId(cart.getUser().getUserId());

        List<CartItemDTO> itemDTOs = cart.getItems().stream()
                .map(this::toItemDTO)
                .collect(Collectors.toList());

        dto.setItems(itemDTOs);
        dto.setTotalAmount(cart.getTotalAmount());
        dto.setTotalItems(cart.getTotalItems());
        return dto;
    }

    // ─── Helper: map CartItemEntity → CartItemDTO ──────────────────────────
    private CartItemDTO toItemDTO(CartItemEntity item) {
        CartItemDTO dto = new CartItemDTO();
        dto.setCartItemId(item.getCartItemId());
        dto.setProductId(item.getProduct().getProductId());
        dto.setProductName(item.getProduct().getName());
        dto.setProductCategory(item.getProduct().getCategory());
        dto.setProductDescription(item.getProduct().getDescription());
        dto.setImageUrl(item.getProduct().getImageUrl());
        dto.setPrice(item.getPrice());
        dto.setSubtotal(item.getSubtotal());
        dto.setQuantity(item.getQuantity());
        dto.setStockQuantity(item.getProduct().getStockQuantity());
        dto.setIsAvailable(item.getProduct().getIsAvailable());
        return dto;
    }

    // ─── Helper: recalculate cart totals ───────────────────────────────────
    private void recalculateTotals(CartEntity cart) {
        BigDecimal total = cart.getItems().stream()
                .map(CartItemEntity::getSubtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        int totalItems = cart.getItems().stream()
                .mapToInt(CartItemEntity::getQuantity)
                .sum();

        cart.setTotalAmount(total);
        cart.setTotalItems(totalItems);
    }

    // ─── GET CART ───────────────────────────────────────────────────────────
    @Transactional  // ← FIX: keeps session open so cart.getItems() works
    public CartDTO getCart(String email) {
        CartEntity cart = getOrCreateCart(email);
        return toDTO(cart);
    }

    // ─── GET CART ITEM COUNT ────────────────────────────────────────────────
    @Transactional  // ← FIX: keeps session open
    public Map<String, Integer> getCartCount(String email) {
        CartEntity cart = getOrCreateCart(email);
        return Map.of("count", cart.getTotalItems());
    }

    // ─── ADD TO CART ────────────────────────────────────────────────────────
    @Transactional
    public CartDTO addToCart(String email, AddToCartRequest request) {
        CartEntity cart = getOrCreateCart(email);

        ProductEntity product = productRepository.findById(request.getProductId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Product not found with id: " + request.getProductId()));

        if (!product.getIsAvailable()) {
            throw new RuntimeException("Product is not available: " + product.getName());
        }

        if (product.getStockQuantity() < request.getQuantity()) {
            throw new RuntimeException("Insufficient stock. Only " +
                    product.getStockQuantity() + " items available.");
        }

        CartItemEntity existingItem = cartItemRepository
                .findByCartAndProduct(cart, product)
                .orElse(null);

        if (existingItem != null) {
            int newQty = existingItem.getQuantity() + request.getQuantity();
            if (newQty > product.getStockQuantity()) {
                throw new RuntimeException("Cannot add more. Only " +
                        product.getStockQuantity() + " items available.");
            }
            existingItem.setQuantity(newQty);
            existingItem.setSubtotal(product.getPrice()
                    .multiply(BigDecimal.valueOf(newQty)));
            cartItemRepository.save(existingItem);
        } else {
            CartItemEntity newItem = new CartItemEntity();
            newItem.setCart(cart);
            newItem.setProduct(product);
            newItem.setQuantity(request.getQuantity());
            newItem.setPrice(product.getPrice());
            newItem.setSubtotal(product.getPrice()
                    .multiply(BigDecimal.valueOf(request.getQuantity())));
            cart.getItems().add(newItem);
            cartItemRepository.save(newItem);
        }

        recalculateTotals(cart);
        CartEntity saved = cartRepository.save(cart);
        return toDTO(saved);
    }

    // ─── UPDATE CART ITEM QUANTITY ──────────────────────────────────────────
    @Transactional
    public CartDTO updateCartItem(String email, Long cartItemId, Integer quantity) {
        CartEntity cart = getOrCreateCart(email);

        CartItemEntity item = cartItemRepository.findById(cartItemId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Cart item not found with id: " + cartItemId));

        if (!item.getCart().getCartId().equals(cart.getCartId())) {
            throw new RuntimeException("Cart item does not belong to your cart.");
        }

        if (quantity < 1) {
            throw new RuntimeException("Quantity must be at least 1.");
        }

        ProductEntity product = item.getProduct();
        if (quantity > product.getStockQuantity()) {
            throw new RuntimeException("Only " + product.getStockQuantity() + " items in stock.");
        }

        item.setQuantity(quantity);
        item.setSubtotal(item.getPrice().multiply(BigDecimal.valueOf(quantity)));
        cartItemRepository.save(item);

        recalculateTotals(cart);
        CartEntity saved = cartRepository.save(cart);
        return toDTO(saved);
    }

    // ─── REMOVE CART ITEM ───────────────────────────────────────────────────
    @Transactional
    public CartDTO removeCartItem(String email, Long cartItemId) {
        CartEntity cart = getOrCreateCart(email);

        CartItemEntity item = cartItemRepository.findById(cartItemId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Cart item not found with id: " + cartItemId));

        if (!item.getCart().getCartId().equals(cart.getCartId())) {
            throw new RuntimeException("Cart item does not belong to your cart.");
        }

        cart.getItems().remove(item);
        cartItemRepository.delete(item);

        recalculateTotals(cart);
        CartEntity saved = cartRepository.save(cart);
        return toDTO(saved);
    }

    // ─── CLEAR CART ─────────────────────────────────────────────────────────
    @Transactional
    public void clearCart(String email) {
        CartEntity cart = getOrCreateCart(email);
        cart.getItems().clear();
        cart.setTotalAmount(BigDecimal.ZERO);
        cart.setTotalItems(0);
        cartRepository.save(cart);
    }

    // ─── GET CART ITEMS (for order creation) ───────────────────────────────
    @Transactional  // ← FIX: keeps session open for callers (OrderService)
    public List<CartItemEntity> getCartItems(String email) {
        CartEntity cart = getOrCreateCart(email);
        // Force-initialize the items list while session is still open
        cart.getItems().size();
        return cart.getItems();
    }
}
