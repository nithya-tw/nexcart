package com.nexcart.productservice.brand.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nexcart.productservice.brand.dto.request.CreateBrandRequest;
import com.nexcart.productservice.brand.dto.response.BrandResponse;
import com.nexcart.productservice.brand.service.BrandService;
import com.nexcart.productservice.exception.DuplicateResourceException;
import com.nexcart.productservice.exception.ResourceNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Unit tests for BrandController.
 * Tests REST API endpoints for brand management using MockMvc.
 */
@WebMvcTest(BrandController.class)
@DisplayName("BrandController Tests")
class BrandControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private BrandService brandService;

    @Autowired
    private ObjectMapper objectMapper;

    private BrandResponse brandResponse;
    private CreateBrandRequest createBrandRequest;

    @BeforeEach
    void setUp() {
        brandResponse = new BrandResponse(
                1L,
                "Apple",
                "Premium technology products",
                "https://example.com/logos/apple.png",
                "https://www.apple.com",
                true,
                LocalDateTime.now(),
                LocalDateTime.now()
        );

        createBrandRequest = new CreateBrandRequest(
                "Apple",
                "Premium technology products",
                "https://example.com/logos/apple.png",
                "https://www.apple.com"
        );
    }

    // ==================== CREATE TESTS ====================

    @Test
    @DisplayName("POST /api/v1/brands - Should create brand successfully")
    void shouldCreateBrand() throws Exception {
        // Given
        when(brandService.createBrand(any(CreateBrandRequest.class)))
                .thenReturn(brandResponse);

        // When & Then
        mockMvc.perform(post("/api/v1/brands")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createBrandRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.name").value("Apple"))
                .andExpect(jsonPath("$.description").value("Premium technology products"))
                .andExpect(jsonPath("$.logoUrl").value("https://example.com/logos/apple.png"))
                .andExpect(jsonPath("$.websiteUrl").value("https://www.apple.com"))
                .andExpect(jsonPath("$.isActive").value(true));

        verify(brandService, times(1)).createBrand(any(CreateBrandRequest.class));
    }

    @Test
    @DisplayName("POST /api/v1/brands - Should create brand with minimal info")
    void shouldCreateBrandWithMinimalInfo() throws Exception {
        // Given
        CreateBrandRequest minimalRequest = new CreateBrandRequest(
                "Samsung",
                null,
                null,
                null
        );

        BrandResponse minimalResponse = new BrandResponse(
                2L,
                "Samsung",
                null,
                null,
                null,
                true,
                LocalDateTime.now(),
                LocalDateTime.now()
        );

        when(brandService.createBrand(any(CreateBrandRequest.class)))
                .thenReturn(minimalResponse);

        // When & Then
        mockMvc.perform(post("/api/v1/brands")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(minimalRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Samsung"))
                .andExpect(jsonPath("$.description").doesNotExist())
                .andExpect(jsonPath("$.logoUrl").doesNotExist())
                .andExpect(jsonPath("$.websiteUrl").doesNotExist());

        verify(brandService, times(1)).createBrand(any(CreateBrandRequest.class));
    }

    @Test
    @DisplayName("POST /api/v1/brands - Should return 400 when name is blank")
    void shouldReturn400WhenNameIsBlank() throws Exception {
        // Given
        CreateBrandRequest invalidRequest = new CreateBrandRequest(
                "",
                "Description",
                null,
                null
        );

        // When & Then
        mockMvc.perform(post("/api/v1/brands")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest());

        verify(brandService, never()).createBrand(any());
    }

    // Note: Brand creation does not check for duplicates in service layer
    // Duplicate check is enforced by database unique constraint

    // ==================== READ TESTS ====================

    @Test
    @DisplayName("GET /api/v1/brands/{id} - Should return brand by ID")
    void shouldReturnBrandById() throws Exception {
        // Given
        Long brandId = 1L;
        when(brandService.getBrandById(brandId)).thenReturn(brandResponse);

        // When & Then
        mockMvc.perform(get("/api/v1/brands/{id}", brandId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.name").value("Apple"))
                .andExpect(jsonPath("$.description").value("Premium technology products"));

        verify(brandService, times(1)).getBrandById(brandId);
    }

    @Test
    @DisplayName("GET /api/v1/brands/{id} - Should return 500 when brand not found")
    void shouldReturn500WhenBrandNotFound() throws Exception {
        // Given
        Long nonExistentId = 999L;
        when(brandService.getBrandById(nonExistentId))
                .thenThrow(new RuntimeException("Brand not found with ID: 999"));

        // When & Then
        mockMvc.perform(get("/api/v1/brands/{id}", nonExistentId))
                .andExpect(status().isInternalServerError());

        verify(brandService, times(1)).getBrandById(nonExistentId);
    }

    @Test
    @DisplayName("GET /api/v1/brands - Should return all brands")
    void shouldReturnAllBrands() throws Exception {
        // Given
        BrandResponse brand2 = new BrandResponse(
                2L,
                "Samsung",
                "Korean electronics",
                null,
                null,
                true,
                LocalDateTime.now(),
                LocalDateTime.now()
        );

        List<BrandResponse> brands = Arrays.asList(brandResponse, brand2);
        when(brandService.getAllBrands()).thenReturn(brands);

        // When & Then
        mockMvc.perform(get("/api/v1/brands"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].name").value("Apple"))
                .andExpect(jsonPath("$[1].name").value("Samsung"));

        verify(brandService, times(1)).getAllBrands();
    }

    @Test
    @DisplayName("GET /api/v1/brands - Should return empty list when no brands")
    void shouldReturnEmptyListWhenNoBrands() throws Exception {
        // Given
        when(brandService.getAllBrands()).thenReturn(Arrays.asList());

        // When & Then
        mockMvc.perform(get("/api/v1/brands"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));

        verify(brandService, times(1)).getAllBrands();
    }

    @Test
    @DisplayName("GET /api/v1/brands/active - Should return only active brands")
    void shouldReturnOnlyActiveBrands() throws Exception {
        // Given
        List<BrandResponse> activeBrands = Arrays.asList(brandResponse);
        when(brandService.getActiveBrands()).thenReturn(activeBrands);

        // When & Then
        mockMvc.perform(get("/api/v1/brands/active"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].isActive").value(true))
                .andExpect(jsonPath("$[0].name").value("Apple"));

        verify(brandService, times(1)).getActiveBrands();
    }

    @Test
    @DisplayName("GET /api/v1/brands/active - Should return empty list when no active brands")
    void shouldReturnEmptyListWhenNoActiveBrands() throws Exception {
        // Given
        when(brandService.getActiveBrands()).thenReturn(Arrays.asList());

        // When & Then
        mockMvc.perform(get("/api/v1/brands/active"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));

        verify(brandService, times(1)).getActiveBrands();
    }

    // ==================== VALIDATION TESTS ====================

    @Test
    @DisplayName("Should serialize brand response with all fields")
    void shouldSerializeBrandResponseWithAllFields() throws Exception {
        // Given
        when(brandService.getBrandById(1L)).thenReturn(brandResponse);

        // When & Then
        mockMvc.perform(get("/api/v1/brands/{id}", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.name").exists())
                .andExpect(jsonPath("$.description").exists())
                .andExpect(jsonPath("$.logoUrl").exists())
                .andExpect(jsonPath("$.websiteUrl").exists())
                .andExpect(jsonPath("$.isActive").exists())
                .andExpect(jsonPath("$.createdAt").exists())
                .andExpect(jsonPath("$.updatedAt").exists());
    }

    @Test
    @DisplayName("Should handle null optional fields in JSON response")
    void shouldHandleNullOptionalFields() throws Exception {
        // Given
        BrandResponse minimalBrand = new BrandResponse(
                2L,
                "GenericBrand",
                null,
                null,
                null,
                true,
                LocalDateTime.now(),
                LocalDateTime.now()
        );

        when(brandService.getBrandById(2L)).thenReturn(minimalBrand);

        // When & Then
        mockMvc.perform(get("/api/v1/brands/{id}", 2L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("GenericBrand"))
                .andExpect(jsonPath("$.description").doesNotExist())
                .andExpect(jsonPath("$.logoUrl").doesNotExist())
                .andExpect(jsonPath("$.websiteUrl").doesNotExist());
    }

    // ==================== VALIDATION TESTS ====================

    @Test
    @DisplayName("POST /api/v1/brands - Should validate name is not null")
    void shouldValidateNameNotNull() throws Exception {
        // Given
        CreateBrandRequest invalidRequest = new CreateBrandRequest(
                null,
                "Description",
                null,
                null
        );

        // When & Then
        mockMvc.perform(post("/api/v1/brands")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest());

        verify(brandService, never()).createBrand(any());
    }

    @Test
    @DisplayName("POST /api/v1/brands - Should accept valid URL formats")
    void shouldAcceptValidUrls() throws Exception {
        // Given
        CreateBrandRequest requestWithUrls = new CreateBrandRequest(
                "Sony",
                "Electronics",
                "https://cdn.example.com/sony.svg",
                "https://www.sony.com"
        );

        BrandResponse responseWithUrls = new BrandResponse(
                3L,
                "Sony",
                "Electronics",
                "https://cdn.example.com/sony.svg",
                "https://www.sony.com",
                true,
                LocalDateTime.now(),
                LocalDateTime.now()
        );

        when(brandService.createBrand(any(CreateBrandRequest.class)))
                .thenReturn(responseWithUrls);

        // When & Then
        mockMvc.perform(post("/api/v1/brands")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestWithUrls)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.logoUrl").value(org.hamcrest.Matchers.startsWith("https://")))
                .andExpect(jsonPath("$.websiteUrl").value(org.hamcrest.Matchers.startsWith("https://")));

        verify(brandService, times(1)).createBrand(any(CreateBrandRequest.class));
    }

    @Test
    @DisplayName("POST /api/v1/brands - Should accept null optional fields")
    void shouldAcceptNullOptionalFields() throws Exception {
        // Given
        CreateBrandRequest requestWithNulls = new CreateBrandRequest(
                "NewBrand",
                null,
                null,
                null
        );

        BrandResponse responseWithNulls = new BrandResponse(
                4L,
                "NewBrand",
                null,
                null,
                null,
                true,
                LocalDateTime.now(),
                LocalDateTime.now()
        );

        when(brandService.createBrand(any(CreateBrandRequest.class)))
                .thenReturn(responseWithNulls);

        // When & Then
        mockMvc.perform(post("/api/v1/brands")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestWithNulls)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("NewBrand"));

        verify(brandService, times(1)).createBrand(any(CreateBrandRequest.class));
    }

}
