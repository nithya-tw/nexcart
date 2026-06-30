package com.nexcart.productservice.category.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nexcart.productservice.category.dto.request.CreateCategoryRequest;
import com.nexcart.productservice.category.dto.response.CategoryResponse;
import com.nexcart.productservice.category.service.CategoryService;
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
 * Unit tests for CategoryController.
 * Tests REST API endpoints for category management using MockMvc.
 */
@WebMvcTest(CategoryController.class)
@DisplayName("CategoryController Tests")
class CategoryControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private CategoryService categoryService;

    @Autowired
    private ObjectMapper objectMapper;

    private CategoryResponse rootCategoryResponse;
    private CategoryResponse subcategoryResponse;
    private CreateCategoryRequest createCategoryRequest;

    @BeforeEach
    void setUp() {
        rootCategoryResponse = new CategoryResponse(
                1L,
                "Electronics",
                "electronics",
                "Electronic devices and gadgets",
                null,
                null,
                0,
                true,
                LocalDateTime.now(),
                LocalDateTime.now()
        );

        subcategoryResponse = new CategoryResponse(
                2L,
                "Smartphones",
                "smartphones",
                "Mobile phones",
                1L,
                null,
                0,
                true,
                LocalDateTime.now(),
                LocalDateTime.now()
        );

        createCategoryRequest = new CreateCategoryRequest(
                "Electronics",
                "Electronic devices and gadgets",
                null,
                null,
                null
        );
    }

    // ==================== CREATE TESTS ====================

    @Test
    @DisplayName("POST /api/v1/categories - Should create category successfully")
    void shouldCreateCategory() throws Exception {
        // Given
        when(categoryService.createCategory(any(CreateCategoryRequest.class)))
                .thenReturn(rootCategoryResponse);

        // When & Then
        mockMvc.perform(post("/api/v1/categories")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createCategoryRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.name").value("Electronics"))
                .andExpect(jsonPath("$.slug").value("electronics"))
                .andExpect(jsonPath("$.description").value("Electronic devices and gadgets"))
                .andExpect(jsonPath("$.parentId").doesNotExist())
                .andExpect(jsonPath("$.isActive").value(true));

        verify(categoryService, times(1)).createCategory(any(CreateCategoryRequest.class));
    }

    @Test
    @DisplayName("POST /api/v1/categories - Should return 400 when name is blank")
    void shouldReturn400WhenNameIsBlank() throws Exception {
        // Given
        CreateCategoryRequest invalidRequest = new CreateCategoryRequest(
                "",
                "Description",
                null,
                null,
                null
        );

        // When & Then
        mockMvc.perform(post("/api/v1/categories")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest());

        verify(categoryService, never()).createCategory(any());
    }

    @Test
    @DisplayName("POST /api/v1/categories - Should create subcategory with parent")
    void shouldCreateSubcategory() throws Exception {
        // Given
        CreateCategoryRequest subcategoryRequest = new CreateCategoryRequest(
                "Smartphones",
                "Mobile phones",
                1L,
                null,
                null
        );

        when(categoryService.createCategory(any(CreateCategoryRequest.class)))
                .thenReturn(subcategoryResponse);

        // When & Then
        mockMvc.perform(post("/api/v1/categories")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(subcategoryRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(2))
                .andExpect(jsonPath("$.name").value("Smartphones"))
                .andExpect(jsonPath("$.slug").value("smartphones"))
                .andExpect(jsonPath("$.parentId").value(1));

        verify(categoryService, times(1)).createCategory(any(CreateCategoryRequest.class));
    }

    // ==================== READ TESTS ====================

    @Test
    @DisplayName("GET /api/v1/categories/{id} - Should return category by ID")
    void shouldReturnCategoryById() throws Exception {
        // Given
        Long categoryId = 1L;
        when(categoryService.getCategoryById(categoryId)).thenReturn(rootCategoryResponse);

        // When & Then
        mockMvc.perform(get("/api/v1/categories/{id}", categoryId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.name").value("Electronics"))
                .andExpect(jsonPath("$.slug").value("electronics"));

        verify(categoryService, times(1)).getCategoryById(categoryId);
    }

    @Test
    @DisplayName("GET /api/v1/categories/{id} - Should return 500 when category not found")
    void shouldReturn500WhenCategoryNotFound() throws Exception {
        // Given
        Long nonExistentId = 999L;
        when(categoryService.getCategoryById(nonExistentId))
                .thenThrow(new RuntimeException("Category not found with ID: 999"));

        // When & Then
        mockMvc.perform(get("/api/v1/categories/{id}", nonExistentId))
                .andExpect(status().isInternalServerError());

        verify(categoryService, times(1)).getCategoryById(nonExistentId);
    }

    @Test
    @DisplayName("GET /api/v1/categories - Should return all categories")
    void shouldReturnAllCategories() throws Exception {
        // Given
        List<CategoryResponse> categories = Arrays.asList(rootCategoryResponse, subcategoryResponse);
        when(categoryService.getAllCategories()).thenReturn(categories);

        // When & Then
        mockMvc.perform(get("/api/v1/categories"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].name").value("Electronics"))
                .andExpect(jsonPath("$[1].name").value("Smartphones"));

        verify(categoryService, times(1)).getAllCategories();
    }

    @Test
    @DisplayName("GET /api/v1/categories - Should return empty list when no categories")
    void shouldReturnEmptyListWhenNoCategories() throws Exception {
        // Given
        when(categoryService.getAllCategories()).thenReturn(Arrays.asList());

        // When & Then
        mockMvc.perform(get("/api/v1/categories"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));

        verify(categoryService, times(1)).getAllCategories();
    }

    @Test
    @DisplayName("GET /api/v1/categories/active - Should return only active categories")
    void shouldReturnOnlyActiveCategories() throws Exception {
        // Given
        List<CategoryResponse> activeCategories = Arrays.asList(rootCategoryResponse);
        when(categoryService.getActiveCategories()).thenReturn(activeCategories);

        // When & Then
        mockMvc.perform(get("/api/v1/categories/active"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].isActive").value(true));

        verify(categoryService, times(1)).getActiveCategories();
    }

    // ==================== VALIDATION TESTS ====================

    @Test
    @DisplayName("POST /api/v1/categories - Should validate name is not null")
    void shouldValidateNameNotNull() throws Exception {
        // Given
        CreateCategoryRequest invalidRequest = new CreateCategoryRequest(
                null,
                "Description",
                null,
                null,
                null
        );

        // When & Then
        mockMvc.perform(post("/api/v1/categories")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest());

        verify(categoryService, never()).createCategory(any());
    }

    @Test
    @DisplayName("POST /api/v1/categories - Should accept null description")
    void shouldAcceptNullDescription() throws Exception {
        // Given
        CreateCategoryRequest requestWithoutDescription = new CreateCategoryRequest(
                "Electronics",
                null,
                null,
                null,
                null
        );

        CategoryResponse responseWithoutDescription = new CategoryResponse(
                1L,
                "Electronics",
                "electronics",
                null,
                null,
                null,
                0,
                true,
                LocalDateTime.now(),
                LocalDateTime.now()
        );

        when(categoryService.createCategory(any(CreateCategoryRequest.class)))
                .thenReturn(responseWithoutDescription);

        // When & Then
        mockMvc.perform(post("/api/v1/categories")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestWithoutDescription)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.description").doesNotExist());

        verify(categoryService, times(1)).createCategory(any(CreateCategoryRequest.class));
    }
}
