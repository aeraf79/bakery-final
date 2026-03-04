package com.example.myproject.controller;

import com.example.myproject.dto.ProductDTO;
import com.example.myproject.entity.ProductEntity;
import com.example.myproject.service.ProductService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/products")

public class ProductController {

    @Autowired
    private ProductService productService;

    // POST /api/products/add  (ADMIN only)
    @PostMapping("/add")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ProductEntity> createProduct(@Valid @RequestBody ProductDTO dto) {
        return new ResponseEntity<>(productService.createProduct(dto), HttpStatus.CREATED);
    }

    // POST /api/products/bulk  (ADMIN only)
    @PostMapping("/bulk")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<ProductEntity>> createMultipleProducts(
            @Valid @RequestBody List<ProductDTO> dtos) {
        return new ResponseEntity<>(productService.createMultipleProducts(dtos), HttpStatus.CREATED);
    }

    // GET /api/products/all  (Public)
    @GetMapping("/all")
    public ResponseEntity<List<ProductEntity>> getAllProducts() {
        return ResponseEntity.ok(productService.getAllProducts());
    }

    // GET /api/products/{id}  (Public)
    @GetMapping("/{id}")
    public ResponseEntity<ProductEntity> getProductById(@PathVariable Long id) {
        return ResponseEntity.ok(productService.getProductById(id));
    }

    // PUT /api/products/{id}  (ADMIN only)
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ProductEntity> updateProduct(@PathVariable Long id,
                                                       @Valid @RequestBody ProductDTO dto) {
        return ResponseEntity.ok(productService.updateProduct(id, dto));
    }

    // DELETE /api/products/{id}  (ADMIN only)
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, String>> deleteProduct(@PathVariable Long id) {
        productService.deleteProduct(id);
        Map<String, String> response = new HashMap<>();
        response.put("message", "Product deleted successfully");
        return ResponseEntity.ok(response);
    }

    // GET /api/products/category/{category}  (Public)
    @GetMapping("/category/{category}")
    public ResponseEntity<List<ProductEntity>> getProductsByCategory(@PathVariable String category) {
        return ResponseEntity.ok(productService.getProductsByCategory(category));
    }

    // GET /api/products/available  (Public)
    @GetMapping("/available")
    public ResponseEntity<List<ProductEntity>> getAvailableProducts() {
        return ResponseEntity.ok(productService.getAvailableProducts());
    }

    // GET /api/products/search?query=  (Public)
    @GetMapping("/search")
    public ResponseEntity<List<ProductEntity>> searchProducts(@RequestParam String query) {
        return ResponseEntity.ok(productService.searchProducts(query));
    }

    // GET /api/products/low-stock?threshold=10  (ADMIN only)
    @GetMapping("/low-stock")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<ProductEntity>> getLowStockProducts(
            @RequestParam(defaultValue = "10") Integer threshold) {
        return ResponseEntity.ok(productService.getLowStockProducts(threshold));
    }

    // PATCH /api/products/{id}/stock?quantity=  (ADMIN only)
    @PatchMapping("/{id}/stock")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ProductEntity> updateStock(@PathVariable Long id,
                                                     @RequestParam Integer quantity) {
        return ResponseEntity.ok(productService.updateStock(id, quantity));
    }

    // PATCH /api/products/{id}/toggle-availability  (ADMIN only)
    @PatchMapping("/{id}/toggle-availability")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ProductEntity> toggleAvailability(@PathVariable Long id) {
        return ResponseEntity.ok(productService.toggleAvailability(id));
    }

    // GET /api/products/price-range?minPrice=&maxPrice=  (Public)
    @GetMapping("/price-range")
    public ResponseEntity<List<ProductEntity>> getProductsByPriceRange(
            @RequestParam BigDecimal minPrice,
            @RequestParam BigDecimal maxPrice) {
        return ResponseEntity.ok(productService.getProductsByPriceRange(minPrice, maxPrice));
    }

    // GET /api/products/statistics  (ADMIN only)
    @GetMapping("/statistics")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> getProductStatistics() {
        List<ProductEntity> all = productService.getAllProducts();
        List<ProductEntity> available = productService.getAvailableProducts();
        List<ProductEntity> lowStock = productService.getLowStockProducts(10);

        Map<String, Object> stats = new HashMap<>();
        stats.put("totalProducts", all.size());
        stats.put("availableProducts", available.size());
        stats.put("outOfStock", all.size() - available.size());
        stats.put("lowStockCount", lowStock.size());
        return ResponseEntity.ok(stats);
    }
}