package com.nexcart.productservice.product.service;

import com.nexcart.productservice.product.dto.request.CreateProductRequest;
import com.nexcart.productservice.product.dto.response.ProductResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.util.List;

public interface ProductService {

    ProductResponse createProduct(CreateProductRequest request);

    ProductResponse getProductById(Long id);

    ProductResponse getProductBySku(String sku);

    ProductResponse getProductBySlug(String slug);

    List<ProductResponse> getAllProducts();

    Page<ProductResponse> getAllProducts(Pageable pageable);

    List<ProductResponse> getActiveProducts();

    List<ProductResponse> getFeaturedProducts();

    List<ProductResponse> getProductsByBrand(Long brandId);

    List<ProductResponse> getProductsByCategory(Long categoryId);

    List<ProductResponse> searchProducts(String query);

    Page<ProductResponse> searchProducts(String query, Pageable pageable);

    List<ProductResponse> getProductsByCategoryAndPriceRange(Long categoryId, BigDecimal minPrice, BigDecimal maxPrice);

    ProductResponse updateProduct(Long id, CreateProductRequest request);

    void deleteProduct(Long id);
}
