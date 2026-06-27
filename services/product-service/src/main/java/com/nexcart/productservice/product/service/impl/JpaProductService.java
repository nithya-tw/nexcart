package com.nexcart.productservice.product.service.impl;

import com.nexcart.productservice.brand.repository.BrandRepository;
import com.nexcart.productservice.category.repository.CategoryRepository;
import com.nexcart.productservice.exception.DuplicateResourceException;
import com.nexcart.productservice.exception.ResourceNotFoundException;
import com.nexcart.productservice.product.dto.request.CreateProductRequest;
import com.nexcart.productservice.product.dto.response.ProductResponse;
import com.nexcart.productservice.product.entity.Product;
import com.nexcart.productservice.product.repository.ProductRepository;
import com.nexcart.productservice.product.service.ProductService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class JpaProductService implements ProductService {

    private final ProductRepository productRepository;
    private final BrandRepository brandRepository;
    private final CategoryRepository categoryRepository;

    @Override
    @Transactional
    public ProductResponse createProduct(CreateProductRequest request) {
        log.info("Creating product with SKU: {}", request.sku());

        if (productRepository.findBySku(request.sku()).isPresent()) {
            throw new DuplicateResourceException("Product with SKU " + request.sku() + " already exists");
        }

        if (!brandRepository.existsById(request.brandId())) {
            throw new ResourceNotFoundException("Brand not found with ID: " + request.brandId());
        }

        if (!categoryRepository.existsById(request.categoryId())) {
            throw new ResourceNotFoundException("Category not found with ID: " + request.categoryId());
        }

        String slug = generateSlug(request.name());

        Product product = Product.builder()
                .sku(request.sku())
                .name(request.name())
                .slug(slug)
                .description(request.description())
                .shortDescription(request.shortDescription())
                .price(request.price())
                .compareAtPrice(request.compareAtPrice())
                .costPrice(request.costPrice())
                .brandId(request.brandId())
                .categoryId(request.categoryId())
                .attributes(request.attributes())
                .metaTitle(request.metaTitle())
                .metaDescription(request.metaDescription())
                .isFeatured(request.isFeatured() != null ? request.isFeatured() : false)
                .isActive(true)
                .build();

        Product saved = productRepository.save(product);
        log.info("Product created successfully with ID: {}", saved.getId());

        return mapToResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public ProductResponse getProductById(Long id) {
        log.info("Fetching product with ID: {}", id);
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with ID: " + id));
        return mapToResponse(product);
    }

    @Override
    @Transactional(readOnly = true)
    public ProductResponse getProductBySku(String sku) {
        log.info("Fetching product with SKU: {}", sku);
        Product product = productRepository.findBySku(sku)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with SKU: " + sku));
        return mapToResponse(product);
    }

    @Override
    @Transactional(readOnly = true)
    public ProductResponse getProductBySlug(String slug) {
        log.info("Fetching product with slug: {}", slug);
        Product product = productRepository.findBySlug(slug)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with slug: " + slug));
        return mapToResponse(product);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProductResponse> getAllProducts() {
        log.info("Fetching all products");
        return productRepository.findAll().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProductResponse> getActiveProducts() {
        log.info("Fetching active products");
        return productRepository.findByIsActiveTrue().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProductResponse> getFeaturedProducts() {
        log.info("Fetching featured products");
        return productRepository.findByIsFeaturedTrueAndIsActiveTrue().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProductResponse> getProductsByBrand(Long brandId) {
        log.info("Fetching products for brand ID: {}", brandId);
        return productRepository.findByBrandIdAndIsActiveTrue(brandId).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProductResponse> getProductsByCategory(Long categoryId) {
        log.info("Fetching products for category ID: {}", categoryId);
        return productRepository.findByCategoryIdAndIsActiveTrue(categoryId).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProductResponse> searchProducts(String query) {
        log.info("Searching products with query: {}", query);
        return productRepository.searchProducts(query).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProductResponse> getProductsByCategoryAndPriceRange(Long categoryId, BigDecimal minPrice, BigDecimal maxPrice) {
        log.info("Fetching products for category {} with price range {}-{}", categoryId, minPrice, maxPrice);
        return productRepository.findByCategoryAndPriceRange(categoryId, minPrice, maxPrice).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public ProductResponse updateProduct(Long id, CreateProductRequest request) {
        log.info("Updating product with ID: {}", id);

        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with ID: " + id));

        if (!product.getSku().equals(request.sku()) &&
                productRepository.findBySku(request.sku()).isPresent()) {
            throw new DuplicateResourceException("Product with SKU " + request.sku() + " already exists");
        }

        product.setSku(request.sku());
        product.setName(request.name());
        product.setSlug(generateSlug(request.name()));
        product.setDescription(request.description());
        product.setShortDescription(request.shortDescription());
        product.setPrice(request.price());
        product.setCompareAtPrice(request.compareAtPrice());
        product.setCostPrice(request.costPrice());
        product.setBrandId(request.brandId());
        product.setCategoryId(request.categoryId());
        product.setAttributes(request.attributes());
        product.setMetaTitle(request.metaTitle());
        product.setMetaDescription(request.metaDescription());
        product.setIsFeatured(request.isFeatured() != null ? request.isFeatured() : false);

        Product updated = productRepository.save(product);
        log.info("Product updated successfully with ID: {}", updated.getId());

        return mapToResponse(updated);
    }

    @Override
    @Transactional
    public void deleteProduct(Long id) {
        log.info("Soft deleting product with ID: {}", id);
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with ID: " + id));

        product.setIsActive(false);
        productRepository.save(product);
        log.info("Product soft deleted successfully with ID: {}", id);
    }

    private ProductResponse mapToResponse(Product product) {
        return ProductResponse.builder()
                .id(product.getId())
                .sku(product.getSku())
                .name(product.getName())
                .slug(product.getSlug())
                .description(product.getDescription())
                .shortDescription(product.getShortDescription())
                .price(product.getPrice())
                .compareAtPrice(product.getCompareAtPrice())
                .costPrice(product.getCostPrice())
                .brandId(product.getBrandId())
                .categoryId(product.getCategoryId())
                .attributes(product.getAttributes())
                .metaTitle(product.getMetaTitle())
                .metaDescription(product.getMetaDescription())
                .isActive(product.getIsActive())
                .isFeatured(product.getIsFeatured())
                .createdAt(product.getCreatedAt())
                .updatedAt(product.getUpdatedAt())
                .build();
    }

    private String generateSlug(String name) {
        return name.toLowerCase()
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("^-|-$", "");
    }
}
