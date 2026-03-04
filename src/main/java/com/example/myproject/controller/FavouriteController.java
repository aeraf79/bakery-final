package com.example.myproject.controller;

import com.example.myproject.dto.FavouriteDTO;
import com.example.myproject.service.FavouriteService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/favourites")
public class FavouriteController {

    @Autowired
    private FavouriteService favouriteService;

    // GET /api/favourites
    // Returns all favourited products for the logged-in user
    @GetMapping
    public ResponseEntity<List<FavouriteDTO>> getFavourites(Principal principal) {
        return ResponseEntity.ok(favouriteService.getFavourites(principal.getName()));
    }

    // POST /api/favourites/toggle/{productId}
    // Adds if not favourited, removes if already favourited
    @PostMapping("/toggle/{productId}")
    public ResponseEntity<Map<String, Object>> toggleFavourite(
            Principal principal,
            @PathVariable Long productId) {
        return ResponseEntity.ok(favouriteService.toggleFavourite(principal.getName(), productId));
    }

    // GET /api/favourites/check/{productId}
    // Returns { isFavourited: true/false } for a specific product
    @GetMapping("/check/{productId}")
    public ResponseEntity<Map<String, Boolean>> checkFavourite(
            Principal principal,
            @PathVariable Long productId) {
        boolean result = favouriteService.isFavourited(principal.getName(), productId);
        return ResponseEntity.ok(Map.of("isFavourited", result));
    }

    // GET /api/favourites/count
    // Returns { count: N }
    @GetMapping("/count")
    public ResponseEntity<Map<String, Long>> getFavouriteCount(Principal principal) {
        return ResponseEntity.ok(favouriteService.getFavouriteCount(principal.getName()));
    }

    // GET /api/favourites/ids
    // Returns list of all favourited productIds — used for bulk heart highlighting in UI
    @GetMapping("/ids")
    public ResponseEntity<List<Long>> getFavouritedProductIds(Principal principal) {
        return ResponseEntity.ok(favouriteService.getFavouritedProductIds(principal.getName()));
    }

    // DELETE /api/favourites/{favouriteId}
    // Remove a single favourite by its favouriteId
    @DeleteMapping("/{favouriteId}")
    public ResponseEntity<Map<String, String>> removeFavourite(
            Principal principal,
            @PathVariable Long favouriteId) {
        favouriteService.removeFavourite(principal.getName(), favouriteId);
        return ResponseEntity.ok(Map.of("message", "Removed from favourites"));
    }

    // DELETE /api/favourites/clear
    // Clear all favourites for the user
    @DeleteMapping("/clear")
    public ResponseEntity<Map<String, String>> clearFavourites(Principal principal) {
        favouriteService.clearAllFavourites(principal.getName());
        return ResponseEntity.ok(Map.of("message", "All favourites cleared"));
    }
}