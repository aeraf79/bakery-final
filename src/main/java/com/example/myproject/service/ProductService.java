package com.example.myproject.service;

import com.example.myproject.dto.ProductDTO;
import com.example.myproject.entity.ProductEntity;
import com.example.myproject.exception.ResourceNotFoundException;
import com.example.myproject.repository.ProductRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;

@Service
public class ProductService {

    @Autowired
    private ProductRepository productRepository;

    private ProductEntity toEntity(ProductDTO dto) {
        ProductEntity product = new ProductEntity();
        product.setName(dto.getName());
        product.setDescription(dto.getDescription());
        product.setPrice(dto.getPrice());
        product.setCategory(dto.getCategory());
        product.setStockQuantity(dto.getStockQuantity());
        product.setImageUrl(dto.getImageUrl());
        product.setIsAvailable(dto.getIsAvailable() != null ? dto.getIsAvailable() : true);
        product.setUnit(dto.getUnit());
        product.setWeight(dto.getWeight());
        product.setAllergens(dto.getAllergens());
        return product;
    }

    public ProductEntity createProduct(ProductDTO dto) {
        return productRepository.save(toEntity(dto));
    }

    public List<ProductEntity> createMultipleProducts(List<ProductDTO> dtos) {
        List<ProductEntity> products = dtos.stream().map(this::toEntity).toList();
        return productRepository.saveAll(products);
    }

    public List<ProductEntity> getAllProducts() {
        return productRepository.findAll();
    }

    public ProductEntity getProductById(Long id) {
        return productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + id));
    }

    public ProductEntity updateProduct(Long id, ProductDTO dto) {
        ProductEntity existing = getProductById(id);
        existing.setName(dto.getName());
        existing.setDescription(dto.getDescription());
        existing.setPrice(dto.getPrice());
        existing.setCategory(dto.getCategory());
        existing.setStockQuantity(dto.getStockQuantity());
        existing.setImageUrl(dto.getImageUrl());
        existing.setIsAvailable(dto.getIsAvailable());
        existing.setUnit(dto.getUnit());
        existing.setWeight(dto.getWeight());
        existing.setAllergens(dto.getAllergens());
        return productRepository.save(existing);
    }

    public void deleteProduct(Long id) {
        if (!productRepository.existsById(id)) {
            throw new ResourceNotFoundException("Product not found with id: " + id);
        }
        productRepository.deleteById(id);
    }

    public List<ProductEntity> getProductsByCategory(String category) {
        return productRepository.findByCategory(category);
    }

    public List<ProductEntity> getAvailableProducts() {
        return productRepository.findByIsAvailableTrue();
    }

    public List<ProductEntity> searchProducts(String query) {
        return productRepository.searchProducts(query);
    }

    public List<ProductEntity> getLowStockProducts(Integer threshold) {
        return productRepository.findLowStockProducts(threshold);
    }

    public ProductEntity updateStock(Long id, Integer quantity) {
        ProductEntity product = getProductById(id);
        product.setStockQuantity(quantity);
        product.setIsAvailable(quantity > 0);
        return productRepository.save(product);
    }

    public ProductEntity toggleAvailability(Long id) {
        ProductEntity product = getProductById(id);
        product.setIsAvailable(!product.getIsAvailable());
        return productRepository.save(product);
    }

    public List<ProductEntity> getProductsByPriceRange(BigDecimal minPrice, BigDecimal maxPrice) {
        return productRepository.findByPriceBetween(minPrice, maxPrice);
    }
}