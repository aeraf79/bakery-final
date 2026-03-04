package com.example.myproject.controller;

import com.example.myproject.dto.AddToCartRequest;
import com.example.myproject.dto.CartDTO;
import com.example.myproject.service.CartService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.Map;

@RestController
@RequestMapping("/api/cart")

public class CartController {

    @Autowired
    private CartService cartService;

    // GET /api/cart  ← Cart.jsx fetchCart()
    @GetMapping
    public ResponseEntity<CartDTO> getCart(Principal principal) {
        CartDTO cart = cartService.getCart(principal.getName());
        return ResponseEntity.ok(cart);
    }

    // GET /api/cart/count  ← UserPanel.jsx (optional, now works)
    @GetMapping("/count")
    public ResponseEntity<Map<String, Integer>> getCartCount(Principal principal) {
        return ResponseEntity.ok(cartService.getCartCount(principal.getName()));
    }

    // POST /api/cart/add  ← UserPanel.jsx handleAddToCart()
    @PostMapping("/add")
    public ResponseEntity<CartDTO> addToCart(
            Principal principal,
            @Valid @RequestBody AddToCartRequest request) {
        CartDTO cart = cartService.addToCart(principal.getName(), request);
        return new ResponseEntity<>(cart, HttpStatus.OK);
    }

    // PUT /api/cart/items/{cartItemId}?quantity=N  ← Cart.jsx updateQuantity()
    @PutMapping("/items/{cartItemId}")
    public ResponseEntity<CartDTO> updateCartItem(
            Principal principal,
            @PathVariable Long cartItemId,
            @RequestParam Integer quantity) {
        CartDTO cart = cartService.updateCartItem(principal.getName(), cartItemId, quantity);
        return ResponseEntity.ok(cart);
    }

    // DELETE /api/cart/items/{cartItemId}  ← Cart.jsx removeItem()
    @DeleteMapping("/items/{cartItemId}")
    public ResponseEntity<CartDTO> removeCartItem(
            Principal principal,
            @PathVariable Long cartItemId) {
        CartDTO cart = cartService.removeCartItem(principal.getName(), cartItemId);
        return ResponseEntity.ok(cart);
    }

    // DELETE /api/cart/clear  ← Cart.jsx clearCart()
    @DeleteMapping("/clear")
    public ResponseEntity<Void> clearCart(Principal principal) {
        cartService.clearCart(principal.getName());
        return ResponseEntity.ok().build();
    }
}