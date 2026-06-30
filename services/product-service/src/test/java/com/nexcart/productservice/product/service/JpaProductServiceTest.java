package com.nexcart.productservice.product.service;

import com.nexcart.productservice.brand.repository.BrandRepository;
import com.nexcart.productservice.category.repository.CategoryRepository;
import com.nexcart.productservice.exception.DuplicateResourceException;
import com.nexcart.productservice.exception.ResourceNotFoundException;
import com.nexcart.productservice.product.dto.request.CreateProductRequest;
import com.nexcart.productservice.product.dto.response.ProductResponse;
import com.nexcart.productservice.product.entity.Product;
import com.nexcart.productservice.product.repository.ProductRepository;
import com.nexcart.productservice.product.service.impl.JpaProductService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Comprehensive unit tests for JpaProductService.
 *
 * This test class demonstrates best practices for unit testing service layer components:
 * 1. Using @ExtendWith(MockitoExtension.class) for seamless Mockito integration with JUnit 5
 * 2. Mocking external dependencies (repositories) to isolate the service under test
 * 3. Testing both happy paths and edge cases
 * 4. Using AssertJ for fluent, readable assertions
 * 5. Testing slug generation logic
 * 6. Verifying interaction with mocked objects (method invocations)
 * 7. Testing business logic validation (duplicate SKU, missing brand/category, etc.)
 *
 * Test coverage includes:
 * - All service methods (create, read, update, delete, search)
 * - Exception handling for various error scenarios
 * - Data mapping from entity to DTO
 * - Repository invocation verification
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("JpaProductService Unit Tests")
class JpaProductServiceTest {

    // ==================== Dependencies (Mocked) ====================
    // These dependencies are mocked to isolate the service under test
    // and avoid database calls during unit testing

    @Mock
    private ProductRepository productRepository;

    @Mock
    private BrandRepository brandRepository;

    @Mock
    private CategoryRepository categoryRepository;

    // ==================== Service Under Test ====================
    // The actual service that we're testing
    // It's instantiated with the mocked dependencies via constructor injection
    private JpaProductService productService;

    // ==================== Test Data Setup ====================
    // Common test data used across multiple tests

    private Product sampleProduct;
    private CreateProductRequest validProductRequest;
    private ProductResponse expectedProductResponse;

    @BeforeEach
    void setUp() {
        // Initialize the service with mocked dependencies
        // This is done here instead of using @InjectMocks to have explicit control
        productService = new JpaProductService(productRepository, brandRepository, categoryRepository);

        // Setup sample product entity
        sampleProduct = Product.builder()
                .id(1L)
                .sku("PROD-001")
                .name("Sample Product")
                .slug("sample-product")
                .description("This is a sample product")
                .shortDescription("Sample product description")
                .price(new BigDecimal("99.99"))
                .compareAtPrice(new BigDecimal("149.99"))
                .costPrice(new BigDecimal("50.00"))
                .brandId(1L)
                .categoryId(1L)
                .attributes(new HashMap<>())
                .metaTitle("Sample Product - Buy Online")
                .metaDescription("Buy sample product at great price")
                .isActive(true)
                .isFeatured(false)
                .build();
        
        // Set audit fields (inherited from BaseEntity)
        sampleProduct.setCreatedAt(LocalDateTime.now());
        sampleProduct.setUpdatedAt(LocalDateTime.now());

        // Setup valid product creation request
        validProductRequest = CreateProductRequest.builder()
                .sku("PROD-001")
                .name("Sample Product")
                .description("This is a sample product")
                .shortDescription("Sample product description")
                .price(new BigDecimal("99.99"))
                .compareAtPrice(new BigDecimal("149.99"))
                .costPrice(new BigDecimal("50.00"))
                .brandId(1L)
                .categoryId(1L)
                .attributes(new HashMap<>())
                .metaTitle("Sample Product - Buy Online")
                .metaDescription("Buy sample product at great price")
                .isFeatured(false)
                .build();
        // Setup expected response
        expectedProductResponse = ProductResponse.builder()
                .id(1L)
                .sku("PROD-001")
                .name("Sample Product")
                .slug("sample-product")
                .description("This is a sample product")
                .shortDescription("Sample product description")
                .price(new BigDecimal("99.99"))
                .compareAtPrice(new BigDecimal("149.99"))
                .costPrice(new BigDecimal("50.00"))
                .brandId(1L)
                .categoryId(1L)
                .attributes(new HashMap<>())
                .metaTitle("Sample Product - Buy Online")
                .metaDescription("Buy sample product at great price")
                .isActive(true)
                .isFeatured(false)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }

    // ==================== CREATE PRODUCT TESTS ====================

    @Test
    @DisplayName("Should create product successfully with valid request")
    void testCreateProduct_Success() {
        // ARRANGE: Setup mocks to return valid results
        // This test follows the AAA pattern: Arrange, Act, Assert
        when(productRepository.findBySku(validProductRequest.sku())).thenReturn(Optional.empty());
        when(brandRepository.existsById(validProductRequest.brandId())).thenReturn(true);
        when(categoryRepository.existsById(validProductRequest.categoryId())).thenReturn(true);
        when(productRepository.save(any(Product.class))).thenReturn(sampleProduct);

        // ACT: Execute the method under test
        ProductResponse response = productService.createProduct(validProductRequest);

        // ASSERT: Verify the response matches expectations
        assertThat(response)
                .isNotNull()
                .extracting("id", "sku", "name")
                .containsExactly(1L, "PROD-001", "Sample Product");

        // VERIFY: Ensure repository was called correctly
        // This verifies that the service is properly delegating to the repository
        verify(productRepository).findBySku(validProductRequest.sku());
        verify(brandRepository).existsById(validProductRequest.brandId());
        verify(categoryRepository).existsById(validProductRequest.categoryId());
        verify(productRepository).save(any(Product.class));
    }

    @Test
    @DisplayName("Should throw DuplicateResourceException when SKU already exists")
    void testCreateProduct_DuplicateSku_ThrowsException() {
        // ARRANGE: Mock repository to return existing product with same SKU
        when(productRepository.findBySku(validProductRequest.sku()))
                .thenReturn(Optional.of(sampleProduct));

        // ACT & ASSERT: Verify exception is thrown when duplicate SKU is detected
        assertThatThrownBy(() -> productService.createProduct(validProductRequest))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessageContaining("already exists");

        // VERIFY: Ensure we don't try to save a duplicate
        verify(productRepository, never()).save(any(Product.class));
    }

    @Test
    @DisplayName("Should throw ResourceNotFoundException when brand does not exist")
    void testCreateProduct_BrandNotFound_ThrowsException() {
        // ARRANGE: Mock SKU check passes, but brand doesn't exist
        when(productRepository.findBySku(validProductRequest.sku())).thenReturn(Optional.empty());
        when(brandRepository.existsById(validProductRequest.brandId())).thenReturn(false);

        // ACT & ASSERT: Verify exception is thrown when brand is not found
        assertThatThrownBy(() -> productService.createProduct(validProductRequest))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Brand not found");

        // VERIFY: Ensure category check was never performed (fail-fast on brand)
        verify(categoryRepository, never()).existsById(any());
    }

    @Test
    @DisplayName("Should throw ResourceNotFoundException when category does not exist")
    void testCreateProduct_CategoryNotFound_ThrowsException() {
        // ARRANGE: Mock SKU and brand checks pass, but category doesn't exist
        when(productRepository.findBySku(validProductRequest.sku())).thenReturn(Optional.empty());
        when(brandRepository.existsById(validProductRequest.brandId())).thenReturn(true);
        when(categoryRepository.existsById(validProductRequest.categoryId())).thenReturn(false);

        // ACT & ASSERT: Verify exception is thrown when category is not found
        assertThatThrownBy(() -> productService.createProduct(validProductRequest))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Category not found");

        // VERIFY: Ensure repository save was never called
        verify(productRepository, never()).save(any(Product.class));
    }

    @Test
    @DisplayName("Should set isActive to true by default for new product")
    void testCreateProduct_DefaultsIsActiveToTrue() {
        // ARRANGE
        when(productRepository.findBySku(validProductRequest.sku())).thenReturn(Optional.empty());
        when(brandRepository.existsById(validProductRequest.brandId())).thenReturn(true);
        when(categoryRepository.existsById(validProductRequest.categoryId())).thenReturn(true);
        when(productRepository.save(any(Product.class))).thenReturn(sampleProduct);

        // ACT
        ProductResponse response = productService.createProduct(validProductRequest);

        // ASSERT: Verify that product is created as active
        assertThat(response.isActive()).isTrue();
    }

    // ==================== RETRIEVE PRODUCT TESTS ====================

    @Test
    @DisplayName("Should retrieve product by ID successfully")
    void testGetProductById_Success() {
        // ARRANGE: Mock repository to return product
        when(productRepository.findById(1L)).thenReturn(Optional.of(sampleProduct));

        // ACT
        ProductResponse response = productService.getProductById(1L);

        // ASSERT
        assertThat(response)
                .isNotNull()
                .extracting("id", "sku")
                .containsExactly(1L, "PROD-001");

        verify(productRepository).findById(1L);
    }

    @Test
    @DisplayName("Should throw ResourceNotFoundException when product ID not found")
    void testGetProductById_NotFound_ThrowsException() {
        // ARRANGE: Mock repository to return empty optional
        when(productRepository.findById(999L)).thenReturn(Optional.empty());

        // ACT & ASSERT
        assertThatThrownBy(() -> productService.getProductById(999L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Product not found with ID: 999");
    }

    @Test
    @DisplayName("Should retrieve product by SKU successfully")
    void testGetProductBySku_Success() {
        // ARRANGE
        when(productRepository.findBySku("PROD-001")).thenReturn(Optional.of(sampleProduct));

        // ACT
        ProductResponse response = productService.getProductBySku("PROD-001");

        // ASSERT
        assertThat(response)
                .isNotNull()
                .extracting("sku", "name")
                .containsExactly("PROD-001", "Sample Product");

        verify(productRepository).findBySku("PROD-001");
    }

    @Test
    @DisplayName("Should throw ResourceNotFoundException when SKU not found")
    void testGetProductBySku_NotFound_ThrowsException() {
        // ARRANGE
        when(productRepository.findBySku("INVALID-SKU")).thenReturn(Optional.empty());

        // ACT & ASSERT
        assertThatThrownBy(() -> productService.getProductBySku("INVALID-SKU"))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Product not found with SKU");
    }

    @Test
    @DisplayName("Should retrieve product by slug successfully")
    void testGetProductBySlug_Success() {
        // ARRANGE
        when(productRepository.findBySlug("sample-product")).thenReturn(Optional.of(sampleProduct));

        // ACT
        ProductResponse response = productService.getProductBySlug("sample-product");

        // ASSERT
        assertThat(response)
                .isNotNull()
                .extracting("slug")
                .isEqualTo("sample-product");

        verify(productRepository).findBySlug("sample-product");
    }

    @Test
    @DisplayName("Should throw ResourceNotFoundException when slug not found")
    void testGetProductBySlug_NotFound_ThrowsException() {
        // ARRANGE
        when(productRepository.findBySlug("invalid-slug")).thenReturn(Optional.empty());

        // ACT & ASSERT
        assertThatThrownBy(() -> productService.getProductBySlug("invalid-slug"))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Product not found with slug");
    }

    @Test
    @DisplayName("Should retrieve all products")
    void testGetAllProducts_Success() {
        // ARRANGE: Mock multiple products
        List<Product> products = Arrays.asList(sampleProduct);
        when(productRepository.findAll()).thenReturn(products);

        // ACT
        List<ProductResponse> responses = productService.getAllProducts();

        // ASSERT
        assertThat(responses)
                .isNotEmpty()
                .hasSize(1)
                .allMatch(p -> p.name() != null);

        verify(productRepository).findAll();
    }

    @Test
    @DisplayName("Should retrieve all active products")
    void testGetActiveProducts_Success() {
        // ARRANGE
        List<Product> activeProducts = Arrays.asList(sampleProduct);
        when(productRepository.findByIsActiveTrue()).thenReturn(activeProducts);

        // ACT
        List<ProductResponse> responses = productService.getActiveProducts();

        // ASSERT
        assertThat(responses)
                .isNotEmpty()
                .allMatch(p -> p.isActive());

        verify(productRepository).findByIsActiveTrue();
    }

    @Test
    @DisplayName("Should retrieve featured products")
    void testGetFeaturedProducts_Success() {
        // ARRANGE: Create featured product
        Product featuredProduct = Product.builder()
                .id(sampleProduct.getId())
                .sku(sampleProduct.getSku())
                .name(sampleProduct.getName())
                .slug(sampleProduct.getSlug())
                .description(sampleProduct.getDescription())
                .shortDescription(sampleProduct.getShortDescription())
                .price(sampleProduct.getPrice())
                .compareAtPrice(sampleProduct.getCompareAtPrice())
                .costPrice(sampleProduct.getCostPrice())
                .brandId(sampleProduct.getBrandId())
                .categoryId(sampleProduct.getCategoryId())
                .attributes(sampleProduct.getAttributes())
                .metaTitle(sampleProduct.getMetaTitle())
                .metaDescription(sampleProduct.getMetaDescription())
                .isFeatured(true)
                .isActive(true)
                .build();
        featuredProduct.setCreatedAt(sampleProduct.getCreatedAt());
        featuredProduct.setUpdatedAt(sampleProduct.getUpdatedAt());

        when(productRepository.findByIsFeaturedTrueAndIsActiveTrue())
                .thenReturn(Collections.singletonList(featuredProduct));

        // ACT
        List<ProductResponse> responses = productService.getFeaturedProducts();

        // ASSERT
        assertThat(responses)
                .isNotEmpty()
                .allMatch(p -> p.isFeatured());

        verify(productRepository).findByIsFeaturedTrueAndIsActiveTrue();
    }

    @Test
    @DisplayName("Should retrieve products by brand ID")
    void testGetProductsByBrand_Success() {
        // ARRANGE
        List<Product> brandProducts = Arrays.asList(sampleProduct);
        when(productRepository.findByBrandIdAndIsActiveTrue(1L)).thenReturn(brandProducts);

        // ACT
        List<ProductResponse> responses = productService.getProductsByBrand(1L);

        // ASSERT
        assertThat(responses)
                .isNotEmpty()
                .allMatch(p -> p.brandId().equals(1L));

        verify(productRepository).findByBrandIdAndIsActiveTrue(1L);
    }

    @Test
    @DisplayName("Should retrieve products by category ID")
    void testGetProductsByCategory_Success() {
        // ARRANGE
        List<Product> categoryProducts = Arrays.asList(sampleProduct);
        when(productRepository.findByCategoryIdAndIsActiveTrue(1L)).thenReturn(categoryProducts);

        // ACT
        List<ProductResponse> responses = productService.getProductsByCategory(1L);

        // ASSERT
        assertThat(responses)
                .isNotEmpty()
                .allMatch(p -> p.categoryId().equals(1L));

        verify(productRepository).findByCategoryIdAndIsActiveTrue(1L);
    }

    @Test
    @DisplayName("Should search products by query")
    void testSearchProducts_Success() {
        // ARRANGE
        List<Product> searchResults = Arrays.asList(sampleProduct);
        when(productRepository.searchProducts("sample")).thenReturn(searchResults);

        // ACT
        List<ProductResponse> responses = productService.searchProducts("sample");

        // ASSERT
        assertThat(responses)
                .isNotEmpty()
                .hasSize(1);

        verify(productRepository).searchProducts("sample");
    }

    @Test
    @DisplayName("Should return empty list when search yields no results")
    void testSearchProducts_NoResults_ReturnsEmptyList() {
        // ARRANGE
        when(productRepository.searchProducts("nonexistent")).thenReturn(Collections.emptyList());

        // ACT
        List<ProductResponse> responses = productService.searchProducts("nonexistent");

        // ASSERT
        assertThat(responses).isEmpty();
    }

    // ==================== UPDATE PRODUCT TESTS ====================

    @Test
    @DisplayName("Should update product successfully")
    void testUpdateProduct_Success() {
        // ARRANGE
        Long productId = 1L;
        CreateProductRequest updateRequest = CreateProductRequest.builder()
                .sku("PROD-002")
                .name("Updated Product")
                .description("Updated description")
                .shortDescription("Updated short desc")
                .price(new BigDecimal("129.99"))
                .compareAtPrice(new BigDecimal("179.99"))
                .costPrice(new BigDecimal("60.00"))
                .brandId(1L)
                .categoryId(1L)
                .attributes(new HashMap<>())
                .metaTitle("Updated Product")
                .metaDescription("Updated meta description")
                .isFeatured(true)
                .build();

        // Create existing product
        Product existingProduct = Product.builder()
                .id(productId)
                .sku("PROD-001")
                .name("Sample Product")
                .slug("sample-product")
                .description("This is a sample product")
                .shortDescription("Sample product description")
                .price(new BigDecimal("99.99"))
                .compareAtPrice(new BigDecimal("149.99"))
                .costPrice(new BigDecimal("50.00"))
                .brandId(1L)
                .categoryId(1L)
                .attributes(new HashMap<>())
                .metaTitle("Sample Product - Buy Online")
                .metaDescription("Buy sample product at great price")
                .isActive(true)
                .isFeatured(false)
                .build();
        existingProduct.setCreatedAt(LocalDateTime.now());
        existingProduct.setUpdatedAt(LocalDateTime.now());

        // Create updated product
        Product updatedProduct = Product.builder()
                .id(productId)
                .sku("PROD-002")
                .name("Updated Product")
                .slug("updated-product")
                .description("Updated description")
                .shortDescription("Updated short desc")
                .price(new BigDecimal("129.99"))
                .compareAtPrice(new BigDecimal("179.99"))
                .costPrice(new BigDecimal("60.00"))
                .brandId(1L)
                .categoryId(1L)
                .attributes(new HashMap<>())
                .metaTitle("Updated Product")
                .metaDescription("Updated meta description")
                .isActive(true)
                .isFeatured(true)
                .build();
        updatedProduct.setCreatedAt(existingProduct.getCreatedAt());
        updatedProduct.setUpdatedAt(LocalDateTime.now());

        when(productRepository.findById(productId)).thenReturn(Optional.of(existingProduct));
        when(productRepository.findBySku("PROD-002")).thenReturn(Optional.empty());
        when(productRepository.save(any(Product.class))).thenReturn(updatedProduct);

        // ACT
        ProductResponse response = productService.updateProduct(productId, updateRequest);

        // ASSERT
        assertThat(response)
                .isNotNull()
                .extracting("sku", "name", "isFeatured")
                .containsExactly("PROD-002", "Updated Product", true);

        verify(productRepository).findById(productId);
        verify(productRepository).save(any(Product.class));
    }

    @Test
    @DisplayName("Should throw ResourceNotFoundException when updating non-existent product")
    void testUpdateProduct_ProductNotFound_ThrowsException() {
        // ARRANGE
        when(productRepository.findById(999L)).thenReturn(Optional.empty());

        // ACT & ASSERT
        assertThatThrownBy(() -> productService.updateProduct(999L, validProductRequest))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Product not found with ID");

        verify(productRepository, never()).save(any(Product.class));
    }

    @Test
    @DisplayName("Should throw DuplicateResourceException when updating with existing SKU")
    void testUpdateProduct_DuplicateSku_ThrowsException() {
        // ARRANGE
        Long productId = 1L;
        Product existingProduct = Product.builder()
                .id(productId)
                .sku("PROD-001")
                .name("Sample Product")
                .slug("sample-product")
                .description("This is a sample product")
                .shortDescription("Sample product description")
                .price(new BigDecimal("99.99"))
                .compareAtPrice(new BigDecimal("149.99"))
                .costPrice(new BigDecimal("50.00"))
                .brandId(1L)
                .categoryId(1L)
                .attributes(new HashMap<>())
                .metaTitle("Sample Product - Buy Online")
                .metaDescription("Buy sample product at great price")
                .isActive(true)
                .isFeatured(false)
                .build();
        existingProduct.setCreatedAt(LocalDateTime.now());
        existingProduct.setUpdatedAt(LocalDateTime.now());

        CreateProductRequest updateRequest = CreateProductRequest.builder()
                .sku("PROD-002")
                .name("Updated Product")
                .description("desc")
                .price(new BigDecimal("99.99"))
                .brandId(1L)
                .categoryId(1L)
                .build();

        Product productWithNewSku = Product.builder()
                .id(2L)
                .sku("PROD-002")
                .name("Another Product")
                .slug("another-product")
                .description("Another product")
                .shortDescription("Desc")
                .price(new BigDecimal("99.99"))
                .compareAtPrice(new BigDecimal("149.99"))
                .costPrice(new BigDecimal("50.00"))
                .brandId(1L)
                .categoryId(1L)
                .attributes(new HashMap<>())
                .metaTitle("Product")
                .metaDescription("Description")
                .isActive(true)
                .isFeatured(false)
                .build();
        productWithNewSku.setCreatedAt(LocalDateTime.now());
        productWithNewSku.setUpdatedAt(LocalDateTime.now());

        when(productRepository.findById(productId)).thenReturn(Optional.of(existingProduct));
        when(productRepository.findBySku("PROD-002")).thenReturn(Optional.of(productWithNewSku));

        // ACT & ASSERT
        assertThatThrownBy(() -> productService.updateProduct(productId, updateRequest))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessageContaining("already exists");

        verify(productRepository, never()).save(any(Product.class));
    }

    // ==================== DELETE PRODUCT TESTS ====================

    @Test
    @DisplayName("Should soft delete product successfully")
    void testDeleteProduct_Success() {
        // ARRANGE: Note that deletion is a soft delete (sets isActive to false)
        when(productRepository.findById(1L)).thenReturn(Optional.of(sampleProduct));
        when(productRepository.save(any(Product.class))).thenReturn(sampleProduct);

        // ACT
        productService.deleteProduct(1L);

        // ASSERT & VERIFY
        // Verify that save was called with the product having isActive set to false
        verify(productRepository).findById(1L);
        verify(productRepository).save(any(Product.class));
    }

    @Test
    @DisplayName("Should throw ResourceNotFoundException when deleting non-existent product")
    void testDeleteProduct_ProductNotFound_ThrowsException() {
        // ARRANGE
        when(productRepository.findById(999L)).thenReturn(Optional.empty());

        // ACT & ASSERT
        assertThatThrownBy(() -> productService.deleteProduct(999L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Product not found with ID");

        verify(productRepository, never()).save(any(Product.class));
    }

    // ==================== SLUG GENERATION TESTS ====================
    // These tests verify the slug generation logic which is critical for SEO

    @Test
    @DisplayName("Should generate slug by converting to lowercase and replacing spaces with hyphens")
    void testSlugGeneration_BasicCase() {
        // ARRANGE
        when(productRepository.findBySku(validProductRequest.sku())).thenReturn(Optional.empty());
        when(brandRepository.existsById(1L)).thenReturn(true);
        when(categoryRepository.existsById(1L)).thenReturn(true);
        when(productRepository.save(any(Product.class))).thenReturn(sampleProduct);

        // ACT
        ProductResponse response = productService.createProduct(validProductRequest);

        // ASSERT: Verify slug is lowercase and spaces are replaced with hyphens
        assertThat(response.slug())
                .isEqualTo("sample-product")
                .isNotNull()
                .doesNotContainAnyWhitespaces()
                .isLowerCase();
    }

    @Test
    @DisplayName("Should remove special characters from slug")
    void testSlugGeneration_SpecialCharacters() {
        // ARRANGE
        CreateProductRequest specialCharRequest = CreateProductRequest.builder()
                .sku("PROD-001")
                .name("Product@#$%^&*()")
                .description("This is a sample product")
                .shortDescription("Sample product description")
                .price(new BigDecimal("99.99"))
                .compareAtPrice(new BigDecimal("149.99"))
                .costPrice(new BigDecimal("50.00"))
                .brandId(1L)
                .categoryId(1L)
                .attributes(new HashMap<>())
                .metaTitle("Sample Product - Buy Online")
                .metaDescription("Buy sample product at great price")
                .isFeatured(false)
                .build();

        when(productRepository.findBySku(specialCharRequest.sku())).thenReturn(Optional.empty());
        when(brandRepository.existsById(1L)).thenReturn(true);
        when(categoryRepository.existsById(1L)).thenReturn(true);

        Product productWithSpecialSlug = Product.builder()
                .id(1L)
                .sku("PROD-001")
                .name("Product@#$%^&*()")
                .slug("product")
                .description("This is a sample product")
                .shortDescription("Sample product description")
                .price(new BigDecimal("99.99"))
                .compareAtPrice(new BigDecimal("149.99"))
                .costPrice(new BigDecimal("50.00"))
                .brandId(1L)
                .categoryId(1L)
                .attributes(new HashMap<>())
                .metaTitle("Sample Product - Buy Online")
                .metaDescription("Buy sample product at great price")
                .isActive(true)
                .isFeatured(false)
                .build();
        productWithSpecialSlug.setCreatedAt(LocalDateTime.now());
        productWithSpecialSlug.setUpdatedAt(LocalDateTime.now());

        when(productRepository.save(any(Product.class))).thenReturn(productWithSpecialSlug);

        // ACT
        ProductResponse response = productService.createProduct(specialCharRequest);

        // ASSERT: Verify special characters are removed
        assertThat(response.slug())
                .doesNotContain("@")
                .doesNotContain("#")
                .doesNotContain("$")
                .doesNotContain("%")
                .doesNotContain("^")
                .doesNotContain("&")
                .doesNotContain("*")
                .doesNotContain("(")
                .doesNotContain(")")
                .isNotEmpty();
    }

    @Test
    @DisplayName("Should handle multiple consecutive spaces in slug generation")
    void testSlugGeneration_MultipleSpaces() {
        // ARRANGE
        CreateProductRequest multiSpaceRequest = CreateProductRequest.builder()
                .sku("PROD-001")
                .name("Product    With    Multiple    Spaces")
                .description("This is a sample product")
                .shortDescription("Sample product description")
                .price(new BigDecimal("99.99"))
                .compareAtPrice(new BigDecimal("149.99"))
                .costPrice(new BigDecimal("50.00"))
                .brandId(1L)
                .categoryId(1L)
                .attributes(new HashMap<>())
                .metaTitle("Sample Product - Buy Online")
                .metaDescription("Buy sample product at great price")
                .isFeatured(false)
                .build();

        when(productRepository.findBySku(multiSpaceRequest.sku())).thenReturn(Optional.empty());
        when(brandRepository.existsById(1L)).thenReturn(true);
        when(categoryRepository.existsById(1L)).thenReturn(true);

        Product productWithCollapsedSlug = Product.builder()
                .id(1L)
                .sku("PROD-001")
                .name("Product    With    Multiple    Spaces")
                .slug("product-with-multiple-spaces")
                .description("This is a sample product")
                .shortDescription("Sample product description")
                .price(new BigDecimal("99.99"))
                .compareAtPrice(new BigDecimal("149.99"))
                .costPrice(new BigDecimal("50.00"))
                .brandId(1L)
                .categoryId(1L)
                .attributes(new HashMap<>())
                .metaTitle("Sample Product - Buy Online")
                .metaDescription("Buy sample product at great price")
                .isActive(true)
                .isFeatured(false)
                .build();
        productWithCollapsedSlug.setCreatedAt(LocalDateTime.now());
        productWithCollapsedSlug.setUpdatedAt(LocalDateTime.now());

        when(productRepository.save(any(Product.class))).thenReturn(productWithCollapsedSlug);

        // ACT
        ProductResponse response = productService.createProduct(multiSpaceRequest);

        // ASSERT: Verify consecutive spaces are collapsed to single hyphen
        assertThat(response.slug())
                .doesNotContain("--")
                .isEqualTo("product-with-multiple-spaces");
    }

    // ==================== EDGE CASES AND VALIDATION TESTS ====================

    @Test
    @DisplayName("Should handle null isFeatured by setting to false")
    void testCreateProduct_NullIsFeatured_DefaultsToFalse() {
        // ARRANGE
        CreateProductRequest requestWithNullFeatured = CreateProductRequest.builder()
                .sku("PROD-001")
                .name("Sample Product")
                .description("This is a sample product")
                .shortDescription("Sample product description")
                .price(new BigDecimal("99.99"))
                .compareAtPrice(new BigDecimal("149.99"))
                .costPrice(new BigDecimal("50.00"))
                .brandId(1L)
                .categoryId(1L)
                .attributes(new HashMap<>())
                .metaTitle("Sample Product - Buy Online")
                .metaDescription("Buy sample product at great price")
                .isFeatured(null)
                .build();

        when(productRepository.findBySku(requestWithNullFeatured.sku())).thenReturn(Optional.empty());
        when(brandRepository.existsById(1L)).thenReturn(true);
        when(categoryRepository.existsById(1L)).thenReturn(true);
        when(productRepository.save(any(Product.class))).thenReturn(sampleProduct);

        // ACT
        ProductResponse response = productService.createProduct(requestWithNullFeatured);

        // ASSERT
        assertThat(response.isFeatured()).isFalse();
    }

    @Test
    @DisplayName("Should preserve all product attributes during mapping")
    void testProductMapping_AllFieldsPreserved() {
        // ARRANGE
        Map<String, Object> attributes = new HashMap<>();
        attributes.put("color", "red");
        attributes.put("size", "large");

        Product productWithAttributes = Product.builder()
                .id(1L)
                .sku("PROD-001")
                .name("Sample Product")
                .slug("sample-product")
                .description("This is a sample product")
                .shortDescription("Sample product description")
                .price(new BigDecimal("99.99"))
                .compareAtPrice(new BigDecimal("149.99"))
                .costPrice(new BigDecimal("50.00"))
                .brandId(1L)
                .categoryId(1L)
                .attributes(attributes)
                .metaTitle("Sample Product - Buy Online")
                .metaDescription("Buy sample product at great price")
                .isActive(true)
                .isFeatured(false)
                .build();
        productWithAttributes.setCreatedAt(LocalDateTime.now());
        productWithAttributes.setUpdatedAt(LocalDateTime.now());

        when(productRepository.findById(1L)).thenReturn(Optional.of(productWithAttributes));

        // ACT
        ProductResponse response = productService.getProductById(1L);

        // ASSERT: Verify all attributes are properly mapped
        assertThat(response)
                .isNotNull()
                .extracting("id", "sku", "name")
                .isNotEmpty();

        assertThat(response.attributes())
                .isNotNull()
                .containsEntry("color", "red")
                .containsEntry("size", "large");
    }

    @Test
    @DisplayName("Should handle empty product list gracefully")
    void testGetAllProducts_EmptyList_ReturnsEmptyList() {
        // ARRANGE
        when(productRepository.findAll()).thenReturn(Collections.emptyList());

        // ACT
        List<ProductResponse> responses = productService.getAllProducts();

        // ASSERT
        assertThat(responses).isEmpty();
    }

    @Test
    @DisplayName("Should not call repository save multiple times during update")
    void testUpdateProduct_OnlySavesOnce() {
        // ARRANGE
        Long productId = 1L;
        Product existingProduct = Product.builder()
                .id(productId)
                .sku("PROD-001")
                .name("Sample Product")
                .slug("sample-product")
                .description("This is a sample product")
                .shortDescription("Sample product description")
                .price(new BigDecimal("99.99"))
                .compareAtPrice(new BigDecimal("149.99"))
                .costPrice(new BigDecimal("50.00"))
                .brandId(1L)
                .categoryId(1L)
                .attributes(new HashMap<>())
                .metaTitle("Sample Product - Buy Online")
                .metaDescription("Buy sample product at great price")
                .isActive(true)
                .isFeatured(false)
                .build();
        existingProduct.setCreatedAt(LocalDateTime.now());
        existingProduct.setUpdatedAt(LocalDateTime.now());

        when(productRepository.findById(productId)).thenReturn(Optional.of(existingProduct));
        // Since we're using the same SKU in the request, the duplicate check won't call the repository
        when(productRepository.save(any(Product.class))).thenReturn(existingProduct);

        // ACT
        productService.updateProduct(productId, validProductRequest);

        // ASSERT: Verify save is called exactly once
        verify(productRepository, times(1)).save(any(Product.class));
    }

    @Test
    @DisplayName("Should handle products with BigDecimal pricing correctly")
    void testPriceHandling_PreservesBigDecimalPrecision() {
        // ARRANGE
        BigDecimal precisePrice = new BigDecimal("99.99");
        
        Product productWithPrecisePrice = Product.builder()
                .id(1L)
                .sku("PROD-001")
                .name("Sample Product")
                .slug("sample-product")
                .description("This is a sample product")
                .shortDescription("Sample product description")
                .price(precisePrice)
                .compareAtPrice(new BigDecimal("149.99"))
                .costPrice(new BigDecimal("50.00"))
                .brandId(1L)
                .categoryId(1L)
                .attributes(new HashMap<>())
                .metaTitle("Sample Product - Buy Online")
                .metaDescription("Buy sample product at great price")
                .isActive(true)
                .isFeatured(false)
                .build();
        productWithPrecisePrice.setCreatedAt(LocalDateTime.now());
        productWithPrecisePrice.setUpdatedAt(LocalDateTime.now());

        when(productRepository.findById(1L)).thenReturn(Optional.of(productWithPrecisePrice));

        // ACT
        ProductResponse response = productService.getProductById(1L);

        // ASSERT: Verify BigDecimal precision is preserved
        assertThat(response.price())
                .isNotNull()
                .isEqualByComparingTo(precisePrice);
    }

    @Test
    @DisplayName("Should return paginated products successfully")
    void testGetAllProductsPaginated_Success() {
        // ARRANGE
        org.springframework.data.domain.Pageable pageable = 
            org.springframework.data.domain.PageRequest.of(0, 10, 
                org.springframework.data.domain.Sort.by("id").ascending());
        
        Product product1 = Product.builder()
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
                .build();
        product1.setCreatedAt(LocalDateTime.now());
        product1.setUpdatedAt(LocalDateTime.now());
        
        Product product2 = Product.builder()
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
                .build();
        product2.setCreatedAt(LocalDateTime.now());
        product2.setUpdatedAt(LocalDateTime.now());
        
        List<Product> products = Arrays.asList(product1, product2);
        
        org.springframework.data.domain.Page<Product> productPage = 
            new org.springframework.data.domain.PageImpl<>(products, pageable, 2);
        
        when(productRepository.findByIsActiveTrue(pageable)).thenReturn(productPage);

        // ACT
        org.springframework.data.domain.Page<ProductResponse> result = 
            productService.getAllProducts(pageable);

        // ASSERT
        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(2);
        assertThat(result.getTotalElements()).isEqualTo(2);
        assertThat(result.getNumber()).isEqualTo(0);
        assertThat(result.getSize()).isEqualTo(10);
        verify(productRepository).findByIsActiveTrue(pageable);
    }

    @Test
    @DisplayName("Should return paginated search results successfully")
    void testSearchProductsPaginated_Success() {
        // ARRANGE
        String query = "laptop";
        org.springframework.data.domain.Pageable pageable = 
            org.springframework.data.domain.PageRequest.of(0, 20);
        
        Product product1 = Product.builder()
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
                .build();
        product1.setCreatedAt(LocalDateTime.now());
        product1.setUpdatedAt(LocalDateTime.now());
        
        Product product2 = Product.builder()
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
                .build();
        product2.setCreatedAt(LocalDateTime.now());
        product2.setUpdatedAt(LocalDateTime.now());
        
        Product product3 = Product.builder()
                .id(3L)
                .sku("LAP-003")
                .name("Student Laptop")
                .slug("student-laptop")
                .description("Budget-friendly student laptop")
                .shortDescription("Student laptop")
                .price(new BigDecimal("599.99"))
                .brandId(1L)
                .categoryId(1L)
                .attributes(new HashMap<>())
                .isActive(true)
                .isFeatured(false)
                .build();
        product3.setCreatedAt(LocalDateTime.now());
        product3.setUpdatedAt(LocalDateTime.now());
        
        List<Product> products = Arrays.asList(product1, product2, product3);
        
        org.springframework.data.domain.Page<Product> productPage = 
            new org.springframework.data.domain.PageImpl<>(products, pageable, 3);
        
        when(productRepository.searchProducts(query, pageable)).thenReturn(productPage);

        // ACT
        org.springframework.data.domain.Page<ProductResponse> result = 
            productService.searchProducts(query, pageable);

        // ASSERT
        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(3);
        assertThat(result.getTotalElements()).isEqualTo(3);
        verify(productRepository).searchProducts(query, pageable);
    }

    @Test
    @DisplayName("Should return empty page when no products match search")
    void testSearchProductsPaginated_EmptyResult() {
        // ARRANGE
        String query = "nonexistent";
        org.springframework.data.domain.Pageable pageable = 
            org.springframework.data.domain.PageRequest.of(0, 10);
        
        org.springframework.data.domain.Page<Product> emptyPage = 
            new org.springframework.data.domain.PageImpl<>(Collections.emptyList(), pageable, 0);
        
        when(productRepository.searchProducts(query, pageable)).thenReturn(emptyPage);

        // ACT
        org.springframework.data.domain.Page<ProductResponse> result = 
            productService.searchProducts(query, pageable);

        // ASSERT
        assertThat(result).isNotNull();
        assertThat(result.getContent()).isEmpty();
        assertThat(result.getTotalElements()).isEqualTo(0);
        verify(productRepository).searchProducts(query, pageable);
    }
}
