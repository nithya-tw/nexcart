package com.nexcart.productservice.product.controller;

import com.nexcart.productservice.product.dto.request.CreateProductRequest;
import com.nexcart.productservice.product.dto.response.ProductResponse;
import com.nexcart.productservice.product.service.ProductService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/v1/products")
@RequiredArgsConstructor
public class ProductController {

    private final ProductService productService;

    @PostMapping
    public ResponseEntity<ProductResponse> createProduct(@Valid @RequestBody CreateProductRequest request) {
        log.info("REST request to create product with SKU: {}", request.sku());
        ProductResponse response = productService.createProduct(request);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @GetMapping("/{id}")
    public ResponseEntity<ProductResponse> getProductById(@PathVariable Long id) {
        log.info("REST request to get product by ID: {}", id);
        ProductResponse response = productService.getProductById(id);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/sku/{sku}")
    public ResponseEntity<ProductResponse> getProductBySku(@PathVariable String sku) {
        log.info("REST request to get product by SKU: {}", sku);
        ProductResponse response = productService.getProductBySku(sku);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/slug/{slug}")
    public ResponseEntity<ProductResponse> getProductBySlug(@PathVariable String slug) {
        log.info("REST request to get product by slug: {}", slug);
        ProductResponse response = productService.getProductBySlug(slug);
        return ResponseEntity.ok(response);
    }

    @GetMapping
    public ResponseEntity<?> getAllProducts(
            @RequestParam(required = false) Boolean active,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size,
            @RequestParam(required = false, defaultValue = "id") String sortBy,
            @RequestParam(required = false, defaultValue = "ASC") String sortDirection) {
        
        log.info("REST request to get all products, active filter: {}, page: {}, size: {}", active, page, size);
        
        if (page != null && size != null) {
            Sort.Direction direction = "DESC".equalsIgnoreCase(sortDirection) ? Sort.Direction.DESC : Sort.Direction.ASC;
            Pageable pageable = PageRequest.of(page, size, Sort.by(direction, sortBy));
            Page<ProductResponse> responses = productService.getAllProducts(pageable);
            return ResponseEntity.ok(responses);
        } else {
            List<ProductResponse> responses = (active != null && active)
                    ? productService.getActiveProducts()
                    : productService.getAllProducts();
            return ResponseEntity.ok(responses);
        }
    }

    @GetMapping("/featured")
    public ResponseEntity<List<ProductResponse>> getFeaturedProducts() {
        log.info("REST request to get featured products");
        List<ProductResponse> responses = productService.getFeaturedProducts();
        return ResponseEntity.ok(responses);
    }

    @GetMapping("/brand/{brandId}")
    public ResponseEntity<List<ProductResponse>> getProductsByBrand(@PathVariable Long brandId) {
        log.info("REST request to get products by brand: {}", brandId);
        List<ProductResponse> responses = productService.getProductsByBrand(brandId);
        return ResponseEntity.ok(responses);
    }

    @GetMapping("/category/{categoryId}")
    public ResponseEntity<List<ProductResponse>> getProductsByCategory(@PathVariable Long categoryId) {
        log.info("REST request to get products by category: {}", categoryId);
        List<ProductResponse> responses = productService.getProductsByCategory(categoryId);
        return ResponseEntity.ok(responses);
    }

    @GetMapping("/search")
    public ResponseEntity<?> searchProducts(
            @RequestParam String q,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size) {
        
        log.info("REST request to search products with query: {}, page: {}, size: {}", q, page, size);
        
        if (page != null && size != null) {
            Pageable pageable = PageRequest.of(page, size);
            Page<ProductResponse> responses = productService.searchProducts(q, pageable);
            return ResponseEntity.ok(responses);
        } else {
            List<ProductResponse> responses = productService.searchProducts(q);
            return ResponseEntity.ok(responses);
        }
    }

    @GetMapping("/category/{categoryId}/price-range")
    public ResponseEntity<List<ProductResponse>> getProductsByCategoryAndPriceRange(
            @PathVariable Long categoryId,
            @RequestParam BigDecimal minPrice,
            @RequestParam BigDecimal maxPrice) {
        log.info("REST request to get products by category {} and price range {}-{}", categoryId, minPrice, maxPrice);
        List<ProductResponse> responses = productService.getProductsByCategoryAndPriceRange(categoryId, minPrice, maxPrice);
        return ResponseEntity.ok(responses);
    }

    @PutMapping("/{id}")
    public ResponseEntity<ProductResponse> updateProduct(
            @PathVariable Long id,
            @Valid @RequestBody CreateProductRequest request) {
        log.info("REST request to update product with ID: {}", id);
        ProductResponse response = productService.updateProduct(id, request);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteProduct(@PathVariable Long id) {
        log.info("REST request to delete product with ID: {}", id);
        productService.deleteProduct(id);
        return ResponseEntity.noContent().build();
    }
}
