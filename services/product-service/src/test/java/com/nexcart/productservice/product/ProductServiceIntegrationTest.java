package com.nexcart.productservice.product;

import com.nexcart.productservice.brand.entity.Brand;
import com.nexcart.productservice.brand.repository.BrandRepository;
import com.nexcart.productservice.category.entity.Category;
import com.nexcart.productservice.category.repository.CategoryRepository;
import com.nexcart.productservice.product.dto.request.CreateProductRequest;
import com.nexcart.productservice.product.dto.response.ProductResponse;
import com.nexcart.productservice.product.entity.Product;
import com.nexcart.productservice.product.repository.ProductRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.cache.CacheAutoConfiguration;
import org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration;
import org.springframework.boot.autoconfigure.data.redis.RedisRepositoriesAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for Product Service using Testcontainers.
 * 
 * Tests the complete Product API flow with real PostgreSQL database.
 * Verifies end-to-end functionality including REST endpoints, service layer,
 * repository operations, and data persistence.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
@DisplayName("Product Service Integration Tests")
@org.springframework.test.context.ActiveProfiles("test")
@org.junit.jupiter.api.Tag("integration")
class ProductServiceIntegrationTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("testdb")
            .withUsername("test")
            .withPassword("test");

    @Container
    static org.testcontainers.containers.GenericContainer<?> redis = 
        new org.testcontainers.containers.GenericContainer<>("redis:7-alpine")
            .withExposedPorts(6379);

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private BrandRepository brandRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    private Brand testBrand;
    private Category testCategory;

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        // Configure Redis from Testcontainer
        registry.add("spring.data.redis.host", redis::getHost);
        registry.add("spring.data.redis.port", redis::getFirstMappedPort);
        registry.add("spring.kafka.producer.bootstrap-servers", () -> "localhost:9092");
    }

    @BeforeEach
    void setUp() {
        productRepository.deleteAll();
        brandRepository.deleteAll();
        categoryRepository.deleteAll();

        // Create test brand and category
        testBrand = brandRepository.save(Brand.builder()
                .name("Test Brand")
                .description("Test Brand Description")
                .isActive(true)
                .build());

        testCategory = categoryRepository.save(Category.builder()
                .name("Test Category")
                .slug("test-category")
                .description("Test Category Description")
                .displayOrder(1)
                .isActive(true)
                .build());
    }

    @Test
    @DisplayName("Should create product successfully")
    void shouldCreateProduct() {
        // Given
        CreateProductRequest request = CreateProductRequest.builder()
                .sku("LAPTOP-001")
                .name("High-Performance Laptop")
                .shortDescription("Powerful laptop for professionals")
                .description("Detailed description of the laptop")
                .price(new BigDecimal("1299.99"))
                .compareAtPrice(new BigDecimal("1499.99"))
                .costPrice(new BigDecimal("999.99"))
                .brandId(testBrand.getId())
                .categoryId(testCategory.getId())
                .isFeatured(true)
                .build();

        // When
        ResponseEntity<ProductResponse> response = restTemplate.postForEntity(
                "/api/v1/products",
                request,
                ProductResponse.class
        );

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().name()).isEqualTo("High-Performance Laptop");
        assertThat(response.getBody().price()).isEqualByComparingTo(new BigDecimal("1299.99"));
        assertThat(response.getBody().sku()).isEqualTo("LAPTOP-001");

        // Verify persistence
        List<Product> products = productRepository.findAll();
        assertThat(products).hasSize(1);
        assertThat(products.get(0).getName()).isEqualTo("High-Performance Laptop");
    }

    @Test
    @DisplayName("Should retrieve product by ID")
    void shouldRetrieveProductById() {
        // Given - Create a product first
        Product savedProduct = productRepository.save(Product.builder()
                .sku("PHONE-001")
                .name("Smartphone")
                .slug("smartphone")
                .shortDescription("Latest model")
                .price(new BigDecimal("899.99"))
                .brandId(testBrand.getId())
                .categoryId(testCategory.getId())
                .isActive(true)
                .build());

        // When
        ResponseEntity<ProductResponse> response = restTemplate.getForEntity(
                "/api/v1/products/" + savedProduct.getId(),
                ProductResponse.class
        );

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().id()).isEqualTo(savedProduct.getId());
        assertThat(response.getBody().name()).isEqualTo("Smartphone");
        assertThat(response.getBody().price()).isEqualByComparingTo(new BigDecimal("899.99"));
    }

    @Test
    @DisplayName("Should return 404 when product not found")
    void shouldReturn404WhenProductNotFound() {
        // When
        ResponseEntity<String> response = restTemplate.getForEntity(
                "/api/v1/products/99999",
                String.class
        );

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    @DisplayName("Should update product successfully")
    void shouldUpdateProduct() {
        // Given - Create a product first
        Product savedProduct = productRepository.save(Product.builder()
                .sku("TABLET-001")
                .name("Tablet")
                .slug("tablet")
                .shortDescription("Old description")
                .price(new BigDecimal("499.99"))
                .brandId(testBrand.getId())
                .categoryId(testCategory.getId())
                .isActive(true)
                .build());

        CreateProductRequest updateRequest = CreateProductRequest.builder()
                .sku("TABLET-001")
                .name("Tablet Pro")
                .shortDescription("Updated description")
                .price(new BigDecimal("599.99"))
                .brandId(testBrand.getId())
                .categoryId(testCategory.getId())
                .isFeatured(false)
                .build();

        // When
        ResponseEntity<ProductResponse> response = restTemplate.exchange(
                "/api/v1/products/" + savedProduct.getId(),
                HttpMethod.PUT,
                new HttpEntity<>(updateRequest),
                ProductResponse.class
        );

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().name()).isEqualTo("Tablet Pro");
        assertThat(response.getBody().shortDescription()).isEqualTo("Updated description");
        assertThat(response.getBody().price()).isEqualByComparingTo(new BigDecimal("599.99"));

        // Verify persistence
        Product updatedProduct = productRepository.findById(savedProduct.getId()).orElseThrow();
        assertThat(updatedProduct.getName()).isEqualTo("Tablet Pro");
    }

    @Test
    @DisplayName("Should delete product successfully (soft delete)")
    void shouldDeleteProduct() {
        // Given - Create a product first
        Product savedProduct = productRepository.save(Product.builder()
                .sku("HEADPHONES-001")
                .name("Headphones")
                .slug("headphones")
                .shortDescription("Noise cancelling")
                .price(new BigDecimal("199.99"))
                .brandId(testBrand.getId())
                .categoryId(testCategory.getId())
                .isActive(true)
                .build());

        // When
        restTemplate.delete("/api/v1/products/" + savedProduct.getId());

        // Then - Verify product is soft deleted (isActive = false, but still exists)
        Optional<Product> deletedProduct = productRepository.findById(savedProduct.getId());
        assertThat(deletedProduct).isPresent();
        assertThat(deletedProduct.get().getIsActive()).isFalse();
    }

    @Test
    @DisplayName("Should retrieve all products")
    void shouldRetrieveAllProducts() {
        // Given - Create multiple products
        productRepository.saveAll(List.of(
                Product.builder()
                        .sku("PROD-001")
                        .name("Product 1")
                        .slug("product-1")
                        .shortDescription("Description 1")
                        .price(new BigDecimal("100.00"))
                        .brandId(testBrand.getId())
                        .categoryId(testCategory.getId())
                        .isActive(true)
                        .build(),
                Product.builder()
                        .sku("PROD-002")
                        .name("Product 2")
                        .slug("product-2")
                        .shortDescription("Description 2")
                        .price(new BigDecimal("200.00"))
                        .brandId(testBrand.getId())
                        .categoryId(testCategory.getId())
                        .isActive(true)
                        .build(),
                Product.builder()
                        .sku("PROD-003")
                        .name("Product 3")
                        .slug("product-3")
                        .shortDescription("Description 3")
                        .price(new BigDecimal("300.00"))
                        .brandId(testBrand.getId())
                        .categoryId(testCategory.getId())
                        .isActive(true)
                        .build()
        ));

        // When
        ResponseEntity<ProductResponse[]> response = restTemplate.getForEntity(
                "/api/v1/products",
                ProductResponse[].class
        );

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody()).hasSizeGreaterThanOrEqualTo(3);
    }

    @Test
    @DisplayName("Should validate product creation with invalid data")
    void shouldValidateProductCreation() {
        // Given - Invalid product (missing required fields)
        CreateProductRequest invalidRequest = CreateProductRequest.builder()
                .sku("")  // Empty SKU
                .name("")  // Empty name
                .price(new BigDecimal("-10.00"))  // Negative price
                .brandId(testBrand.getId())
                .categoryId(testCategory.getId())
                .build();

        // When
        ResponseEntity<String> response = restTemplate.postForEntity(
                "/api/v1/products",
                invalidRequest,
                String.class
        );

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        
        // Verify no product was created
        assertThat(productRepository.count()).isZero();
    }

    @Test
    @DisplayName("Should verify database persistence across operations")
    void shouldVerifyDatabasePersistence() {
        // Given - Create initial product
        Product initialProduct = productRepository.save(Product.builder()
                .sku("PERSIST-001")
                .name("Persistence Test Product")
                .slug("persistence-test-product")
                .shortDescription("Testing persistence")
                .price(new BigDecimal("150.00"))
                .brandId(testBrand.getId())
                .categoryId(testCategory.getId())
                .isActive(true)
                .build());

        Long productId = initialProduct.getId();

        // When - Retrieve and verify
        Product retrievedProduct = productRepository.findById(productId).orElseThrow();

        // Then - Verify data integrity
        assertThat(retrievedProduct).isNotNull();
        assertThat(retrievedProduct.getId()).isEqualTo(productId);
        assertThat(retrievedProduct.getName()).isEqualTo("Persistence Test Product");
        assertThat(retrievedProduct.getSku()).isEqualTo("PERSIST-001");
        assertThat(retrievedProduct.getPrice()).isEqualByComparingTo(new BigDecimal("150.00"));
        assertThat(retrievedProduct.getBrandId()).isEqualTo(testBrand.getId());
        assertThat(retrievedProduct.getCategoryId()).isEqualTo(testCategory.getId());
    }

    @Test
    @DisplayName("Should search products by name")
    void shouldSearchProductsByName() {
        // Given - Create products with searchable names
        productRepository.saveAll(List.of(
                Product.builder()
                        .sku("LAPTOP-DELL")
                        .name("Dell XPS 15 Laptop")
                        .slug("dell-xps-15-laptop")
                        .shortDescription("Premium laptop")
                        .price(new BigDecimal("1499.99"))
                        .brandId(testBrand.getId())
                        .categoryId(testCategory.getId())
                        .isActive(true)
                        .build(),
                Product.builder()
                        .sku("LAPTOP-MAC")
                        .name("MacBook Pro Laptop")
                        .slug("macbook-pro-laptop")
                        .shortDescription("Apple laptop")
                        .price(new BigDecimal("2499.99"))
                        .brandId(testBrand.getId())
                        .categoryId(testCategory.getId())
                        .isActive(true)
                        .build(),
                Product.builder()
                        .sku("PHONE-IP")
                        .name("iPhone 15 Pro")
                        .slug("iphone-15-pro")
                        .shortDescription("Apple smartphone")
                        .price(new BigDecimal("999.99"))
                        .brandId(testBrand.getId())
                        .categoryId(testCategory.getId())
                        .isActive(true)
                        .build()
        ));

        // When - Search for "laptop"
        ResponseEntity<ProductResponse[]> response = restTemplate.getForEntity(
                "/api/v1/products/search?q=laptop",
                ProductResponse[].class
        );

        // Then - Should return only laptop products
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody()).hasSize(2);
        assertThat(response.getBody())
                .extracting(ProductResponse::name)
                .containsExactlyInAnyOrder("Dell XPS 15 Laptop", "MacBook Pro Laptop");
        
        // Verify search is case-insensitive
        ResponseEntity<ProductResponse[]> upperCaseResponse = restTemplate.getForEntity(
                "/api/v1/products/search?q=LAPTOP",
                ProductResponse[].class
        );
        assertThat(upperCaseResponse.getBody()).hasSize(2);
    }

    @Test
    @DisplayName("Should filter products by category")
    void shouldFilterProductsByCategory() {
        // Given - Create additional categories
        Category electronicsCategory = categoryRepository.save(Category.builder()
                .name("Electronics")
                .slug("electronics")
                .description("Electronic devices")
                .displayOrder(2)
                .isActive(true)
                .build());

        Category clothingCategory = categoryRepository.save(Category.builder()
                .name("Clothing")
                .slug("clothing")
                .description("Apparel and accessories")
                .displayOrder(3)
                .isActive(true)
                .build());

        // Create products in different categories
        productRepository.saveAll(List.of(
                Product.builder()
                        .sku("LAPTOP-001")
                        .name("Gaming Laptop")
                        .slug("gaming-laptop")
                        .shortDescription("High-end gaming laptop")
                        .price(new BigDecimal("1899.99"))
                        .brandId(testBrand.getId())
                        .categoryId(electronicsCategory.getId())
                        .isActive(true)
                        .build(),
                Product.builder()
                        .sku("PHONE-001")
                        .name("Smartphone")
                        .slug("smartphone")
                        .shortDescription("Latest smartphone")
                        .price(new BigDecimal("799.99"))
                        .brandId(testBrand.getId())
                        .categoryId(electronicsCategory.getId())
                        .isActive(true)
                        .build(),
                Product.builder()
                        .sku("SHIRT-001")
                        .name("Cotton T-Shirt")
                        .slug("cotton-t-shirt")
                        .shortDescription("Comfortable cotton shirt")
                        .price(new BigDecimal("29.99"))
                        .brandId(testBrand.getId())
                        .categoryId(clothingCategory.getId())
                        .isActive(true)
                        .build()
        ));

        // When - Filter by Electronics category
        ResponseEntity<ProductResponse[]> response = restTemplate.getForEntity(
                "/api/v1/products/category/" + electronicsCategory.getId(),
                ProductResponse[].class
        );

        // Then - Should return only electronics products
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody()).hasSize(2);
        assertThat(response.getBody())
                .extracting(ProductResponse::categoryId)
                .containsOnly(electronicsCategory.getId());
        assertThat(response.getBody())
                .extracting(ProductResponse::name)
                .containsExactlyInAnyOrder("Gaming Laptop", "Smartphone");
    }

    @Test
    @DisplayName("Should filter products by brand")
    void shouldFilterProductsByBrand() {
        // Given - Create multiple brands
        Brand appleBrand = brandRepository.save(Brand.builder()
                .name("Apple")
                .description("Apple Inc.")
                .isActive(true)
                .build());

        Brand samsungBrand = brandRepository.save(Brand.builder()
                .name("Samsung")
                .description("Samsung Electronics")
                .isActive(true)
                .build());

        // Create products with different brands
        productRepository.saveAll(List.of(
                Product.builder()
                        .sku("IPHONE-15")
                        .name("iPhone 15 Pro")
                        .slug("iphone-15-pro")
                        .shortDescription("Latest Apple phone")
                        .price(new BigDecimal("1099.99"))
                        .brandId(appleBrand.getId())
                        .categoryId(testCategory.getId())
                        .isActive(true)
                        .build(),
                Product.builder()
                        .sku("MACBOOK-PRO")
                        .name("MacBook Pro 16")
                        .slug("macbook-pro-16")
                        .shortDescription("Apple laptop")
                        .price(new BigDecimal("2799.99"))
                        .brandId(appleBrand.getId())
                        .categoryId(testCategory.getId())
                        .isActive(true)
                        .build(),
                Product.builder()
                        .sku("GALAXY-S24")
                        .name("Galaxy S24 Ultra")
                        .slug("galaxy-s24-ultra")
                        .shortDescription("Samsung flagship phone")
                        .price(new BigDecimal("1199.99"))
                        .brandId(samsungBrand.getId())
                        .categoryId(testCategory.getId())
                        .isActive(true)
                        .build()
        ));

        // When - Filter by Apple brand
        ResponseEntity<ProductResponse[]> response = restTemplate.getForEntity(
                "/api/v1/products/brand/" + appleBrand.getId(),
                ProductResponse[].class
        );

        // Then - Should return only Apple products
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody()).hasSize(2);
        assertThat(response.getBody())
                .extracting(ProductResponse::brandId)
                .containsOnly(appleBrand.getId());
        assertThat(response.getBody())
                .extracting(ProductResponse::name)
                .containsExactlyInAnyOrder("iPhone 15 Pro", "MacBook Pro 16");
    }

    @Test
    @DisplayName("Should filter products by price range within category")
    void shouldFilterProductsByPriceRange() {
        // Given - Create products with different prices
        productRepository.saveAll(List.of(
                Product.builder()
                        .sku("BUDGET-001")
                        .name("Budget Product")
                        .slug("budget-product")
                        .shortDescription("Affordable option")
                        .price(new BigDecimal("49.99"))
                        .brandId(testBrand.getId())
                        .categoryId(testCategory.getId())
                        .isActive(true)
                        .build(),
                Product.builder()
                        .sku("MID-RANGE-001")
                        .name("Mid-range Product")
                        .slug("mid-range-product")
                        .shortDescription("Great value")
                        .price(new BigDecimal("499.99"))
                        .brandId(testBrand.getId())
                        .categoryId(testCategory.getId())
                        .isActive(true)
                        .build(),
                Product.builder()
                        .sku("MID-RANGE-002")
                        .name("Quality Product")
                        .slug("quality-product")
                        .shortDescription("Good quality")
                        .price(new BigDecimal("749.99"))
                        .brandId(testBrand.getId())
                        .categoryId(testCategory.getId())
                        .isActive(true)
                        .build(),
                Product.builder()
                        .sku("PREMIUM-001")
                        .name("Premium Product")
                        .slug("premium-product")
                        .shortDescription("High-end luxury")
                        .price(new BigDecimal("2499.99"))
                        .brandId(testBrand.getId())
                        .categoryId(testCategory.getId())
                        .isActive(true)
                        .build()
        ));

        // When - Filter by price range $100-$1000
        String url = String.format(
                "/api/v1/products/category/%d/price-range?minPrice=100&maxPrice=1000",
                testCategory.getId()
        );
        ResponseEntity<ProductResponse[]> response = restTemplate.getForEntity(
                url,
                ProductResponse[].class
        );

        // Then - Should return only products in the price range
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody()).hasSize(2);
        assertThat(response.getBody())
                .extracting(ProductResponse::name)
                .containsExactlyInAnyOrder("Mid-range Product", "Quality Product");
        
        // Verify all prices are within range
        for (ProductResponse product : response.getBody()) {
            assertThat(product.price())
                    .isGreaterThanOrEqualTo(new BigDecimal("100.00"))
                    .isLessThanOrEqualTo(new BigDecimal("1000.00"));
        }
    }

    @Test
    @DisplayName("Should retrieve only featured products")
    void shouldRetrieveFeaturedProducts() {
        // Given - Create mix of featured and non-featured products
        productRepository.saveAll(List.of(
                Product.builder()
                        .sku("FEATURED-001")
                        .name("Featured Laptop")
                        .slug("featured-laptop")
                        .shortDescription("Top seller")
                        .price(new BigDecimal("1599.99"))
                        .brandId(testBrand.getId())
                        .categoryId(testCategory.getId())
                        .isFeatured(true)
                        .isActive(true)
                        .build(),
                Product.builder()
                        .sku("FEATURED-002")
                        .name("Featured Phone")
                        .slug("featured-phone")
                        .shortDescription("Editor's choice")
                        .price(new BigDecimal("899.99"))
                        .brandId(testBrand.getId())
                        .categoryId(testCategory.getId())
                        .isFeatured(true)
                        .isActive(true)
                        .build(),
                Product.builder()
                        .sku("REGULAR-001")
                        .name("Regular Product")
                        .slug("regular-product")
                        .shortDescription("Standard item")
                        .price(new BigDecimal("299.99"))
                        .brandId(testBrand.getId())
                        .categoryId(testCategory.getId())
                        .isFeatured(false)
                        .isActive(true)
                        .build(),
                Product.builder()
                        .sku("REGULAR-002")
                        .name("Another Regular Product")
                        .slug("another-regular-product")
                        .shortDescription("Another standard item")
                        .price(new BigDecimal("199.99"))
                        .brandId(testBrand.getId())
                        .categoryId(testCategory.getId())
                        .isFeatured(false)
                        .isActive(true)
                        .build()
        ));

        // When - Get featured products
        ResponseEntity<ProductResponse[]> response = restTemplate.getForEntity(
                "/api/v1/products/featured",
                ProductResponse[].class
        );

        // Then - Should return only featured products
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody()).hasSize(2);
        assertThat(response.getBody())
                .extracting(ProductResponse::name)
                .containsExactlyInAnyOrder("Featured Laptop", "Featured Phone");
        
        // Verify all returned products are featured
        for (ProductResponse product : response.getBody()) {
            assertThat(product.isFeatured()).isTrue();
        }
    }
}
