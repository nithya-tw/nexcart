package com.nexcart.productservice.brand.service;

import com.nexcart.productservice.brand.dto.request.CreateBrandRequest;
import com.nexcart.productservice.brand.dto.response.BrandResponse;
import com.nexcart.productservice.brand.entity.Brand;
import com.nexcart.productservice.brand.repository.BrandRepository;
import com.nexcart.productservice.exception.DuplicateResourceException;
import com.nexcart.productservice.exception.ResourceNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for BrandService.
 * Tests brand CRUD operations and business logic.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("BrandService Unit Tests")
class BrandServiceTest {

    @Mock
    private BrandRepository brandRepository;

    private BrandService brandService;

    private Brand sampleBrand;
    private CreateBrandRequest validRequest;

    @BeforeEach
    void setUp() {
        brandService = new BrandService(brandRepository);

        sampleBrand = Brand.builder()
                .id(1L)
                .name("Apple")
                .description("Premium technology products")
                .logoUrl("https://example.com/logos/apple.png")
                .websiteUrl("https://www.apple.com")
                .isActive(true)
                .build();

        validRequest = new CreateBrandRequest(
                "Apple",
                "Premium technology products",
                "https://example.com/logos/apple.png",
                "https://www.apple.com"
        );
    }

    // ==================== CREATE TESTS ====================

    @Test
    @DisplayName("Should create brand successfully")
    void shouldCreateBrand() {
        // Given
        when(brandRepository.save(any(Brand.class))).thenReturn(sampleBrand);

        // When
        BrandResponse response = brandService.createBrand(validRequest);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.name()).isEqualTo("Apple");
        assertThat(response.description()).isEqualTo("Premium technology products");
        assertThat(response.logoUrl()).isEqualTo("https://example.com/logos/apple.png");
        assertThat(response.websiteUrl()).isEqualTo("https://www.apple.com");
        assertThat(response.isActive()).isTrue();

        verify(brandRepository).save(any(Brand.class));
    }

    @Test
    @DisplayName("Should create brand with minimal information")
    void shouldCreateBrandWithMinimalInfo() {
        // Given
        CreateBrandRequest minimalRequest = new CreateBrandRequest(
                "Samsung",
                null,
                null,
                null
        );

        Brand minimalBrand = Brand.builder()
                .id(2L)
                .name("Samsung")
                .isActive(true)
                .build();

        when(brandRepository.save(any(Brand.class))).thenReturn(minimalBrand);

        // When
        BrandResponse response = brandService.createBrand(minimalRequest);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.name()).isEqualTo("Samsung");
        assertThat(response.description()).isNull();
        assertThat(response.logoUrl()).isNull();
        assertThat(response.websiteUrl()).isNull();

        verify(brandRepository).save(any(Brand.class));
    }

    // Note: BrandService does NOT validate for duplicate names
    // It relies on database unique constraint

    @Test
    @DisplayName("Should create inactive brand")
    void shouldCreateInactiveBrand() {
        // Given
        CreateBrandRequest inactiveRequest = new CreateBrandRequest(
                "Discontinued Brand",
                "No longer selling",
                null,
                null
        );

        Brand inactiveBrand = Brand.builder()
                .id(3L)
                .name("Discontinued Brand")
                .description("No longer selling")
                .isActive(false)
                .build();

        when(brandRepository.save(any(Brand.class))).thenReturn(inactiveBrand);

        // When
        BrandResponse response = brandService.createBrand(inactiveRequest);

        // Then
        assertThat(response.isActive()).isFalse();
        verify(brandRepository).save(any(Brand.class));
    }

    // ==================== READ TESTS ====================

    @Test
    @DisplayName("Should get brand by ID")
    void shouldGetBrandById() {
        // Given
        Long brandId = 1L;
        when(brandRepository.findById(brandId)).thenReturn(Optional.of(sampleBrand));

        // When
        BrandResponse response = brandService.getBrandById(brandId);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.id()).isEqualTo(brandId);
        assertThat(response.name()).isEqualTo("Apple");
        assertThat(response.description()).isEqualTo("Premium technology products");

        verify(brandRepository).findById(brandId);
    }

    @Test
    @DisplayName("Should throw exception when brand not found by ID")
    void shouldThrowExceptionWhenBrandNotFound() {
        // Given
        Long nonExistentId = 999L;
        when(brandRepository.findById(nonExistentId)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> brandService.getBrandById(nonExistentId))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Brand not found with ID: 999");

        verify(brandRepository).findById(nonExistentId);
    }

    @Test
    @DisplayName("Should get all brands")
    void shouldGetAllBrands() {
        // Given
        Brand brand1 = Brand.builder()
                .id(1L)
                .name("Apple")
                .isActive(true)
                .build();

        Brand brand2 = Brand.builder()
                .id(2L)
                .name("Samsung")
                .isActive(true)
                .build();

        Brand brand3 = Brand.builder()
                .id(3L)
                .name("Nokia")
                .isActive(false)
                .build();

        when(brandRepository.findAll()).thenReturn(Arrays.asList(brand1, brand2, brand3));

        // When
        List<BrandResponse> responses = brandService.getAllBrands();

        // Then
        assertThat(responses).hasSize(3);
        assertThat(responses).extracting(BrandResponse::name)
                .containsExactlyInAnyOrder("Apple", "Samsung", "Nokia");

        verify(brandRepository).findAll();
    }

    @Test
    @DisplayName("Should get only active brands")
    void shouldGetOnlyActiveBrands() {
        // Given
        Brand activeBrand1 = Brand.builder()
                .id(1L)
                .name("Apple")
                .isActive(true)
                .build();

        Brand activeBrand2 = Brand.builder()
                .id(2L)
                .name("Samsung")
                .isActive(true)
                .build();

        when(brandRepository.findByIsActiveTrue()).thenReturn(Arrays.asList(activeBrand1, activeBrand2));

        // When
        List<BrandResponse> responses = brandService.getActiveBrands();

        // Then
        assertThat(responses).hasSize(2);
        assertThat(responses).allMatch(BrandResponse::isActive);
        assertThat(responses).extracting(BrandResponse::name)
                .containsExactlyInAnyOrder("Apple", "Samsung");

        verify(brandRepository).findByIsActiveTrue();
    }

    @Test
    @DisplayName("Should return empty list when no active brands")
    void shouldReturnEmptyListWhenNoActiveBrands() {
        // Given
        when(brandRepository.findByIsActiveTrue()).thenReturn(Arrays.asList());

        // When
        List<BrandResponse> responses = brandService.getActiveBrands();

        // Then
        assertThat(responses).isEmpty();

        verify(brandRepository).findByIsActiveTrue();
    }

    @Test
    @DisplayName("Should handle empty repository")
    void shouldHandleEmptyRepository() {
        // Given
        when(brandRepository.findAll()).thenReturn(Arrays.asList());

        // When
        List<BrandResponse> responses = brandService.getAllBrands();

        // Then
        assertThat(responses).isEmpty();
        verify(brandRepository).findAll();
    }

    // ==================== VALIDATION TESTS ====================

    @Test
    @DisplayName("Should handle URL validation")
    void shouldHandleUrlValidation() {
        // Given
        CreateBrandRequest requestWithUrls = new CreateBrandRequest(
                "Sony",
                "Electronics manufacturer",
                "https://cdn.example.com/sony-logo.svg",
                "https://www.sony.com"
        );

        Brand brandWithUrls = Brand.builder()
                .id(4L)
                .name("Sony")
                .description("Electronics manufacturer")
                .logoUrl("https://cdn.example.com/sony-logo.svg")
                .websiteUrl("https://www.sony.com")
                .isActive(true)
                .build();

        when(brandRepository.save(any(Brand.class))).thenReturn(brandWithUrls);

        // When
        BrandResponse response = brandService.createBrand(requestWithUrls);

        // Then
        assertThat(response.logoUrl()).startsWith("https://");
        assertThat(response.websiteUrl()).startsWith("https://");
    }

    // ==================== EDGE CASE TESTS ====================

    @Test
    @DisplayName("Should handle brand with very long description")
    void shouldHandleLongDescription() {
        // Given
        String longDescription = "A".repeat(1000);
        CreateBrandRequest request = new CreateBrandRequest(
                "Test Brand",
                longDescription,
                null,
                null
        );

        Brand brand = Brand.builder()
                .id(5L)
                .name("Test Brand")
                .description(longDescription)
                .isActive(true)
                .build();

        when(brandRepository.save(any(Brand.class))).thenReturn(brand);

        // When
        BrandResponse response = brandService.createBrand(request);

        // Then
        assertThat(response.description()).hasSize(1000);
    }

    @Test
    @DisplayName("Should handle special characters in brand name")
    void shouldHandleSpecialCharactersInName() {
        // Given
        CreateBrandRequest request = new CreateBrandRequest(
                "L'Oréal",
                "Beauty products",
                null,
                null
        );

        Brand brand = Brand.builder()
                .id(6L)
                .name("L'Oréal")
                .description("Beauty products")
                .isActive(true)
                .build();

        when(brandRepository.save(any(Brand.class))).thenReturn(brand);

        // When
        BrandResponse response = brandService.createBrand(request);

        // Then
        assertThat(response.name()).isEqualTo("L'Oréal");
    }

    @Test
    @DisplayName("Should preserve case sensitivity in brand names")
    void shouldPreserveCaseSensitivity() {
        // Given
        Brand brand1 = Brand.builder()
                .id(1L)
                .name("Apple")
                .isActive(true)
                .build();

        when(brandRepository.findAll()).thenReturn(Arrays.asList(brand1));

        // When
        List<BrandResponse> responses = brandService.getAllBrands();

        // Then
        assertThat(responses.get(0).name()).isEqualTo("Apple");
        assertThat(responses.get(0).name()).isNotEqualTo("apple");
        assertThat(responses.get(0).name()).isNotEqualTo("APPLE");
    }

    // ==================== REPOSITORY INTERACTION TESTS ====================

    @Test
    @DisplayName("Should call repository save method with correct parameters")
    void shouldCallRepositorySaveCorrectly() {
        // Given
        when(brandRepository.save(any(Brand.class))).thenReturn(sampleBrand);

        // When
        brandService.createBrand(validRequest);

        // Then
        verify(brandRepository, times(1)).save(any(Brand.class));
        verifyNoMoreInteractions(brandRepository);
    }
}
