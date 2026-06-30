package com.nexcart.productservice.product.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nexcart.productservice.product.dto.request.CreateProductRequest;
import com.nexcart.productservice.product.dto.response.ProductResponse;
import com.nexcart.productservice.product.service.ProductService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * ProductControllerTest - Unit tests for ProductController
 *
 * This test class provides comprehensive coverage for the ProductController endpoints.
 * It uses @WebMvcTest to test only the controller layer, mocking the ProductService layer.
 *
 * Test Coverage:
 * - POST /api/v1/products - Create product
 * - GET /api/v1/products - Get all products with optional filter
 * - GET /api/v1/products/{id} - Get product by ID
 * - GET /api/v1/products/sku/{sku} - Get product by SKU
 * - GET /api/v1/products/slug/{slug} - Get product by slug
 * - GET /api/v1/products/featured - Get featured products
 * - GET /api/v1/products/brand/{brandId} - Get products by brand
 * - GET /api/v1/products/category/{categoryId} - Get products by category
 * - GET /api/v1/products/search - Search products
 * - GET /api/v1/products/category/{categoryId}/price-range - Get products by price range
 * - PUT /api/v1/products/{id} - Update product
 * - DELETE /api/v1/products/{id} - Delete product
 */
@WebMvcTest(ProductController.class)
@DisplayName("ProductController Tests")
class ProductControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ProductService productService;

    @Autowired
    private ObjectMapper objectMapper;

    private ProductResponse productResponse;
    private CreateProductRequest createProductRequest;

    /**
     * Setup test data before each test case
     * This method prepares common test fixtures that are reused across multiple tests
     */
    @BeforeEach
    void setUp() {
        // Arrange: Create a sample product response
        productResponse = ProductResponse.builder()
                .id(1L)
                .sku("PROD-001")
                .name("Premium Laptop")
                .slug("premium-laptop")
                .description("High-performance laptop with 16GB RAM")
                .shortDescription("Premium laptop")
                .price(new BigDecimal("999.99"))
                .compareAtPrice(new BigDecimal("1299.99"))
                .costPrice(new BigDecimal("500.00"))
                .brandId(1L)
                .categoryId(1L)
                .attributes(new HashMap<>(Map.of("color", "silver", "processor", "Intel i7")))
                .metaTitle("Premium Laptop - Best Price")
                .metaDescription("Buy premium laptop with 16GB RAM at best price")
                .isActive(true)
                .isFeatured(true)
                .createdAt(LocalDateTime.now().minusDays(30))
                .updatedAt(LocalDateTime.now())
                .build();

        // Arrange: Create a sample create product request
        createProductRequest = CreateProductRequest.builder()
                .sku("PROD-001")
                .name("Premium Laptop")
                .description("High-performance laptop with 16GB RAM")
                .shortDescription("Premium laptop")
                .price(new BigDecimal("999.99"))
                .compareAtPrice(new BigDecimal("1299.99"))
                .costPrice(new BigDecimal("500.00"))
                .brandId(1L)
                .categoryId(1L)
                .attributes(new HashMap<>(Map.of("color", "silver", "processor", "Intel i7")))
                .metaTitle("Premium Laptop - Best Price")
                .metaDescription("Buy premium laptop with 16GB RAM at best price")
                .isFeatured(true)
                .build();
    }

    /**
     * Test Case 1: POST /api/v1/products - Create Product Successfully
     *
     * Scenario: Client sends a valid CreateProductRequest to create a new product
     * Expected: Should return HTTP 201 (Created) with the created product response
     */
    @Test
    @DisplayName("Should create product successfully and return HTTP 201")
    void testCreateProduct_Success() throws Exception {
        // Arrange
        when(productService.createProduct(any(CreateProductRequest.class)))
                .thenReturn(productResponse);

        // Act & Assert
        mockMvc.perform(post("/api/v1/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createProductRequest)))
                .andDo(print())
                .andExpect(status().isCreated())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id", is(1)))
                .andExpect(jsonPath("$.sku", is("PROD-001")))
                .andExpect(jsonPath("$.name", is("Premium Laptop")))
                .andExpect(jsonPath("$.price", is(999.99)))
                .andExpect(jsonPath("$.isFeatured", is(true)));

        // Verify that productService.createProduct was called exactly once
        verify(productService, times(1)).createProduct(any(CreateProductRequest.class));
    }

    /**
     * Test Case 2: POST /api/v1/products - Validation Error
     *
     * Scenario: Client sends invalid CreateProductRequest (missing required fields)
     * Expected: Should return HTTP 400 (Bad Request) due to validation error
     */
    @Test
    @DisplayName("Should return HTTP 400 when product request is invalid")
    void testCreateProduct_ValidationError() throws Exception {
        // Arrange: Create an invalid request (missing required fields)
        String invalidRequest = "{ \"sku\": \"\" }"; // Empty SKU and missing other required fields

        // Act & Assert
        mockMvc.perform(post("/api/v1/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidRequest))
                .andDo(print())
                .andExpect(status().isBadRequest());

        // Verify that productService was not called due to validation error
        verify(productService, never()).createProduct(any(CreateProductRequest.class));
    }

    /**
     * Test Case 3: GET /api/v1/products/{id} - Get Product By ID Successfully
     *
     * Scenario: Client requests a product by valid product ID
     * Expected: Should return HTTP 200 (OK) with the product details
     */
    @Test
    @DisplayName("Should retrieve product by ID successfully")
    void testGetProductById_Success() throws Exception {
        // Arrange
        Long productId = 1L;
        when(productService.getProductById(productId))
                .thenReturn(productResponse);

        // Act & Assert
        mockMvc.perform(get("/api/v1/products/{id}", productId)
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id", is(1)))
                .andExpect(jsonPath("$.name", is("Premium Laptop")))
                .andExpect(jsonPath("$.sku", is("PROD-001")));

        // Verify
        verify(productService, times(1)).getProductById(productId);
    }

    /**
     * Test Case 4: GET /api/v1/products/sku/{sku} - Get Product By SKU Successfully
     *
     * Scenario: Client requests a product by valid SKU
     * Expected: Should return HTTP 200 (OK) with the product details
     */
    @Test
    @DisplayName("Should retrieve product by SKU successfully")
    void testGetProductBySku_Success() throws Exception {
        // Arrange
        String sku = "PROD-001";
        when(productService.getProductBySku(sku))
                .thenReturn(productResponse);

        // Act & Assert
        mockMvc.perform(get("/api/v1/products/sku/{sku}", sku)
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sku", is("PROD-001")))
                .andExpect(jsonPath("$.price", is(999.99)));

        // Verify
        verify(productService, times(1)).getProductBySku(sku);
    }

    /**
     * Test Case 5: GET /api/v1/products/slug/{slug} - Get Product By Slug Successfully
     *
     * Scenario: Client requests a product by valid slug
     * Expected: Should return HTTP 200 (OK) with the product details
     */
    @Test
    @DisplayName("Should retrieve product by slug successfully")
    void testGetProductBySlug_Success() throws Exception {
        // Arrange
        String slug = "premium-laptop";
        when(productService.getProductBySlug(slug))
                .thenReturn(productResponse);

        // Act & Assert
        mockMvc.perform(get("/api/v1/products/slug/{slug}", slug)
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.slug", is("premium-laptop")))
                .andExpect(jsonPath("$.name", is("Premium Laptop")));

        // Verify
        verify(productService, times(1)).getProductBySlug(slug);
    }

    /**
     * Test Case 6: GET /api/v1/products - Get All Products Without Filter
     *
     * Scenario: Client requests all products without any filter parameter
     * Expected: Should return HTTP 200 (OK) with list of all products
     */
    @Test
    @DisplayName("Should retrieve all products successfully")
    void testGetAllProducts_Success() throws Exception {
        // Arrange
        ProductResponse product2 = ProductResponse.builder()
                .id(2L)
                .sku("PROD-002")
                .name("Budget Laptop")
                .price(new BigDecimal("499.99"))
                .brandId(1L)
                .categoryId(1L)
                .isActive(true)
                .isFeatured(false)
                .build();

        List<ProductResponse> productList = Arrays.asList(productResponse, product2);
        when(productService.getAllProducts()).thenReturn(productList);

        // Act & Assert
        mockMvc.perform(get("/api/v1/products")
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].name", is("Premium Laptop")))
                .andExpect(jsonPath("$[1].name", is("Budget Laptop")));

        // Verify
        verify(productService, times(1)).getAllProducts();
        verify(productService, never()).getActiveProducts();
    }

    /**
     * Test Case 7: GET /api/v1/products - Get Active Products With Filter
     *
     * Scenario: Client requests products with active=true filter parameter
     * Expected: Should return HTTP 200 (OK) with list of active products only
     */
    @Test
    @DisplayName("Should retrieve only active products when filter is set")
    void testGetAllProducts_WithActiveFilter() throws Exception {
        // Arrange
        List<ProductResponse> activeProducts = Arrays.asList(productResponse);
        when(productService.getActiveProducts()).thenReturn(activeProducts);

        // Act & Assert
        mockMvc.perform(get("/api/v1/products")
                        .param("active", "true")
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].isActive", is(true)));

        // Verify
        verify(productService, times(1)).getActiveProducts();
        verify(productService, never()).getAllProducts();
    }

    /**
     * Test Case 8: GET /api/v1/products/featured - Get Featured Products Successfully
     *
     * Scenario: Client requests all featured products
     * Expected: Should return HTTP 200 (OK) with list of featured products
     */
    @Test
    @DisplayName("Should retrieve featured products successfully")
    void testGetFeaturedProducts_Success() throws Exception {
        // Arrange
        List<ProductResponse> featuredProducts = Arrays.asList(productResponse);
        when(productService.getFeaturedProducts()).thenReturn(featuredProducts);

        // Act & Assert
        mockMvc.perform(get("/api/v1/products/featured")
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].isFeatured", is(true)));

        // Verify
        verify(productService, times(1)).getFeaturedProducts();
    }

    /**
     * Test Case 9: GET /api/v1/products/brand/{brandId} - Get Products By Brand Successfully
     *
     * Scenario: Client requests products for a specific brand
     * Expected: Should return HTTP 200 (OK) with products from that brand
     */
    @Test
    @DisplayName("Should retrieve products by brand ID successfully")
    void testGetProductsByBrand_Success() throws Exception {
        // Arrange
        Long brandId = 1L;
        List<ProductResponse> brandProducts = Arrays.asList(productResponse);
        when(productService.getProductsByBrand(brandId)).thenReturn(brandProducts);

        // Act & Assert
        mockMvc.perform(get("/api/v1/products/brand/{brandId}", brandId)
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].brandId", is(1)));

        // Verify
        verify(productService, times(1)).getProductsByBrand(brandId);
    }

    /**
     * Test Case 10: GET /api/v1/products/category/{categoryId} - Get Products By Category Successfully
     *
     * Scenario: Client requests products from a specific category
     * Expected: Should return HTTP 200 (OK) with products from that category
     */
    @Test
    @DisplayName("Should retrieve products by category ID successfully")
    void testGetProductsByCategory_Success() throws Exception {
        // Arrange
        Long categoryId = 1L;
        List<ProductResponse> categoryProducts = Arrays.asList(productResponse);
        when(productService.getProductsByCategory(categoryId)).thenReturn(categoryProducts);

        // Act & Assert
        mockMvc.perform(get("/api/v1/products/category/{categoryId}", categoryId)
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].categoryId", is(1)));

        // Verify
        verify(productService, times(1)).getProductsByCategory(categoryId);
    }

    /**
     * Test Case 11: GET /api/v1/products/search - Search Products Successfully
     *
     * Scenario: Client searches for products using a search query
     * Expected: Should return HTTP 200 (OK) with matching products
     */
    @Test
    @DisplayName("Should search products successfully")
    void testSearchProducts_Success() throws Exception {
        // Arrange
        String query = "laptop";
        List<ProductResponse> searchResults = Arrays.asList(productResponse);
        when(productService.searchProducts(query)).thenReturn(searchResults);

        // Act & Assert
        mockMvc.perform(get("/api/v1/products/search")
                        .param("q", query)
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].name", containsString("Laptop")));

        // Verify
        verify(productService, times(1)).searchProducts(query);
    }

    /**
     * Test Case 12: GET /api/v1/products/category/{categoryId}/price-range - Get Products By Price Range Successfully
     *
     * Scenario: Client requests products within a specific price range for a category
     * Expected: Should return HTTP 200 (OK) with products matching the criteria
     */
    @Test
    @DisplayName("Should retrieve products by category and price range successfully")
    void testGetProductsByCategoryAndPriceRange_Success() throws Exception {
        // Arrange
        Long categoryId = 1L;
        BigDecimal minPrice = new BigDecimal("900.00");
        BigDecimal maxPrice = new BigDecimal("1000.00");
        List<ProductResponse> priceRangeProducts = Arrays.asList(productResponse);
        when(productService.getProductsByCategoryAndPriceRange(categoryId, minPrice, maxPrice))
                .thenReturn(priceRangeProducts);

        // Act & Assert
        mockMvc.perform(get("/api/v1/products/category/{categoryId}/price-range", categoryId)
                        .param("minPrice", "900.00")
                        .param("maxPrice", "1000.00")
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].price", is(999.99)));

        // Verify
        verify(productService, times(1))
                .getProductsByCategoryAndPriceRange(categoryId, minPrice, maxPrice);
    }

    /**
     * Test Case 13: PUT /api/v1/products/{id} - Update Product Successfully
     *
     * Scenario: Client sends an update request for an existing product
     * Expected: Should return HTTP 200 (OK) with the updated product details
     */
    @Test
    @DisplayName("Should update product successfully and return HTTP 200")
    void testUpdateProduct_Success() throws Exception {
        // Arrange
        Long productId = 1L;
        CreateProductRequest updateRequest = CreateProductRequest.builder()
                .sku("PROD-001-UPDATED")
                .name("Premium Laptop Pro")
                .description("Updated description")
                .shortDescription("Premium laptop")
                .price(new BigDecimal("1199.99"))
                .compareAtPrice(new BigDecimal("1499.99"))
                .costPrice(new BigDecimal("600.00"))
                .brandId(1L)
                .categoryId(1L)
                .isFeatured(true)
                .build();

        ProductResponse updatedResponse = ProductResponse.builder()
                .id(productId)
                .sku("PROD-001-UPDATED")
                .name("Premium Laptop Pro")
                .description("Updated description")
                .price(new BigDecimal("1199.99"))
                .brandId(1L)
                .categoryId(1L)
                .isActive(true)
                .isFeatured(true)
                .build();

        when(productService.updateProduct(productId, updateRequest))
                .thenReturn(updatedResponse);

        // Act & Assert
        mockMvc.perform(put("/api/v1/products/{id}", productId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(1)))
                .andExpect(jsonPath("$.sku", is("PROD-001-UPDATED")))
                .andExpect(jsonPath("$.name", is("Premium Laptop Pro")))
                .andExpect(jsonPath("$.price", is(1199.99)));

        // Verify
        verify(productService, times(1)).updateProduct(eq(productId), any(CreateProductRequest.class));
    }

    /**
     * Test Case 14: DELETE /api/v1/products/{id} - Delete Product Successfully
     *
     * Scenario: Client sends a delete request for an existing product
     * Expected: Should return HTTP 204 (No Content) with no response body
     */
    @Test
    @DisplayName("Should delete product successfully and return HTTP 204")
    void testDeleteProduct_Success() throws Exception {
        // Arrange
        Long productId = 1L;
        doNothing().when(productService).deleteProduct(productId);

        // Act & Assert
        mockMvc.perform(delete("/api/v1/products/{id}", productId)
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isNoContent());

        // Verify that deleteProduct was called with correct ID
        verify(productService, times(1)).deleteProduct(productId);
    }

    @Test
    @DisplayName("Should return paginated products when page and size parameters are provided")
    void testGetAllProducts_WithPagination() throws Exception {
        // Arrange
        ProductResponse product1 = ProductResponse.builder()
                .id(1L)
                .sku("PROD-001")
                .name("Product 1")
                .slug("product-1")
                .description("Description 1")
                .shortDescription("Short desc 1")
                .price(new BigDecimal("99.99"))
                .brandId(1L)
                .categoryId(1L)
                .attributes(new HashMap<>())
                .isActive(true)
                .isFeatured(false)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
        
        ProductResponse product2 = ProductResponse.builder()
                .id(2L)
                .sku("PROD-002")
                .name("Product 2")
                .slug("product-2")
                .description("Description 2")
                .shortDescription("Short desc 2")
                .price(new BigDecimal("149.99"))
                .brandId(1L)
                .categoryId(1L)
                .attributes(new HashMap<>())
                .isActive(true)
                .isFeatured(false)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
        
        org.springframework.data.domain.Pageable pageable = 
            org.springframework.data.domain.PageRequest.of(0, 10, 
                org.springframework.data.domain.Sort.by(org.springframework.data.domain.Sort.Direction.ASC, "id"));
        
        org.springframework.data.domain.Page<ProductResponse> page = 
            new org.springframework.data.domain.PageImpl<>(
                Arrays.asList(product1, product2), pageable, 2);
        
        when(productService.getAllProducts(any(org.springframework.data.domain.Pageable.class)))
            .thenReturn(page);

        // Act & Assert
        mockMvc.perform(get("/api/v1/products")
                        .param("page", "0")
                        .param("size", "10")
                        .param("sortBy", "id")
                        .param("sortDirection", "ASC")
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(2)))
                .andExpect(jsonPath("$.totalElements", is(2)))
                .andExpect(jsonPath("$.number", is(0)))
                .andExpect(jsonPath("$.size", is(10)));

        verify(productService, times(1)).getAllProducts(any(org.springframework.data.domain.Pageable.class));
    }

    @Test
    @DisplayName("Should return paginated search results when page and size parameters are provided")
    void testSearchProducts_WithPagination() throws Exception {
        // Arrange
        String query = "laptop";
        ProductResponse product1 = ProductResponse.builder()
                .id(1L)
                .sku("LAP-001")
                .name("Gaming Laptop")
                .slug("gaming-laptop")
                .description("High-end gaming laptop")
                .shortDescription("Gaming laptop")
                .price(new BigDecimal("1499.99"))
                .brandId(1L)
                .categoryId(1L)
                .attributes(new HashMap<>())
                .isActive(true)
                .isFeatured(true)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
        
        ProductResponse product2 = ProductResponse.builder()
                .id(2L)
                .sku("LAP-002")
                .name("Business Laptop")
                .slug("business-laptop")
                .description("Professional business laptop")
                .shortDescription("Business laptop")
                .price(new BigDecimal("999.99"))
                .brandId(1L)
                .categoryId(1L)
                .attributes(new HashMap<>())
                .isActive(true)
                .isFeatured(false)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
        
        org.springframework.data.domain.Pageable pageable = 
            org.springframework.data.domain.PageRequest.of(0, 20);
        
        org.springframework.data.domain.Page<ProductResponse> page = 
            new org.springframework.data.domain.PageImpl<>(
                Arrays.asList(product1, product2), pageable, 2);
        
        when(productService.searchProducts(eq(query), any(org.springframework.data.domain.Pageable.class)))
            .thenReturn(page);

        // Act & Assert
        mockMvc.perform(get("/api/v1/products/search")
                        .param("q", query)
                        .param("page", "0")
                        .param("size", "20")
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(2)))
                .andExpect(jsonPath("$.totalElements", is(2)))
                .andExpect(jsonPath("$.content[0].name", is("Gaming Laptop")))
                .andExpect(jsonPath("$.content[1].name", is("Business Laptop")));

        verify(productService, times(1)).searchProducts(eq(query), any(org.springframework.data.domain.Pageable.class));
    }
}
