package com.nexcart.productservice.category.service;

import com.nexcart.productservice.category.dto.request.CreateCategoryRequest;
import com.nexcart.productservice.category.dto.response.CategoryResponse;
import com.nexcart.productservice.category.entity.Category;
import com.nexcart.productservice.category.repository.CategoryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
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
 * Unit tests for CategoryService.
 * Tests category CRUD operations, hierarchical structure, and slug generation.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("CategoryService Unit Tests")
class CategoryServiceTest {

    @Mock
    private CategoryRepository categoryRepository;

    private CategoryService categoryService;

    private Category sampleCategory;
    private CreateCategoryRequest validRequest;

    @BeforeEach
    void setUp() {
        categoryService = new CategoryService(categoryRepository);

        // Setup root category
        sampleCategory = Category.builder()
                .id(1L)
                .name("Electronics")
                .slug("electronics")
                .description("Electronic devices and gadgets")
                .parentId(null)
                .imageUrl(null)
                .displayOrder(0)
                .isActive(true)
                .build();

        validRequest = new CreateCategoryRequest(
                "Electronics",
                "Electronic devices and gadgets",
                null,
                null,
                null
        );
    }

    // ==================== CREATE TESTS ====================

    @Test
    @DisplayName("Should create root category successfully")
    void shouldCreateRootCategory() {
        // Given
        when(categoryRepository.save(any(Category.class))).thenReturn(sampleCategory);

        // When
        CategoryResponse response = categoryService.createCategory(validRequest);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.name()).isEqualTo("Electronics");
        assertThat(response.slug()).isEqualTo("electronics");
        assertThat(response.parentId()).isNull();
        assertThat(response.isActive()).isTrue();

        verify(categoryRepository).save(any(Category.class));
    }

    @Test
    @DisplayName("Should create subcategory with parent")
    void shouldCreateSubcategoryWithParent() {
        // Given
        Long parentId = 1L;

        Category subcategory = Category.builder()
                .id(2L)
                .name("Smartphones")
                .slug("smartphones")
                .parentId(parentId)
                .displayOrder(0)
                .isActive(true)
                .build();

        CreateCategoryRequest subcategoryRequest = new CreateCategoryRequest(
                "Smartphones",
                "Mobile phones",
                parentId,
                null,
                null
        );

        when(categoryRepository.save(any(Category.class))).thenReturn(subcategory);

        // When
        CategoryResponse response = categoryService.createCategory(subcategoryRequest);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.name()).isEqualTo("Smartphones");
        assertThat(response.slug()).isEqualTo("smartphones");
        assertThat(response.parentId()).isEqualTo(parentId);

        verify(categoryRepository).save(any(Category.class));
    }

    @Test
    @DisplayName("Should generate correct slug from category name")
    void shouldGenerateCorrectSlug() {
        // Given
        CreateCategoryRequest request = new CreateCategoryRequest(
                "Men's Clothing",
                "Clothing for men",
                null,
                null,
                null
        );

        Category savedCategory = Category.builder()
                .id(1L)
                .name("Men's Clothing")
                .slug("mens-clothing")
                .displayOrder(0)
                .isActive(true)
                .build();

        when(categoryRepository.save(any(Category.class))).thenReturn(savedCategory);

        // When
        CategoryResponse response = categoryService.createCategory(request);

        // Then
        assertThat(response.slug()).isEqualTo("mens-clothing");

        ArgumentCaptor<Category> categoryCaptor = ArgumentCaptor.forClass(Category.class);
        verify(categoryRepository).save(categoryCaptor.capture());
        assertThat(categoryCaptor.getValue().getSlug()).matches("[a-z0-9-]+");
    }

    // ==================== READ TESTS ====================

    @Test
    @DisplayName("Should get category by ID")
    void shouldGetCategoryById() {
        // Given
        Long categoryId = 1L;
        when(categoryRepository.findById(categoryId)).thenReturn(Optional.of(sampleCategory));

        // When
        CategoryResponse response = categoryService.getCategoryById(categoryId);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.id()).isEqualTo(categoryId);
        assertThat(response.name()).isEqualTo("Electronics");

        verify(categoryRepository).findById(categoryId);
    }

    @Test
    @DisplayName("Should throw exception when category not found by ID")
    void shouldThrowExceptionWhenCategoryNotFound() {
        // Given
        Long nonExistentId = 999L;
        when(categoryRepository.findById(nonExistentId)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> categoryService.getCategoryById(nonExistentId))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Category not found with ID: 999");

        verify(categoryRepository).findById(nonExistentId);
    }

    @Test
    @DisplayName("Should get all categories")
    void shouldGetAllCategories() {
        // Given
        Category category1 = Category.builder()
                .id(1L)
                .name("Electronics")
                .slug("electronics")
                .displayOrder(0)
                .isActive(true)
                .build();

        Category category2 = Category.builder()
                .id(2L)
                .name("Clothing")
                .slug("clothing")
                .displayOrder(0)
                .isActive(true)
                .build();

        when(categoryRepository.findAll()).thenReturn(Arrays.asList(category1, category2));

        // When
        List<CategoryResponse> responses = categoryService.getAllCategories();

        // Then
        assertThat(responses).hasSize(2);
        assertThat(responses).extracting(CategoryResponse::name)
                .containsExactlyInAnyOrder("Electronics", "Clothing");

        verify(categoryRepository).findAll();
    }

    @Test
    @DisplayName("Should get only active categories")
    void shouldGetOnlyActiveCategories() {
        // Given
        Category activeCategory = Category.builder()
                .id(1L)
                .name("Electronics")
                .slug("electronics")
                .displayOrder(0)
                .isActive(true)
                .build();

        when(categoryRepository.findByIsActiveTrue()).thenReturn(Arrays.asList(activeCategory));

        // When
        List<CategoryResponse> responses = categoryService.getActiveCategories();

        // Then
        assertThat(responses).hasSize(1);
        assertThat(responses.get(0).isActive()).isTrue();
        assertThat(responses.get(0).name()).isEqualTo("Electronics");

        verify(categoryRepository).findByIsActiveTrue();
    }

    @Test
    @DisplayName("Should return empty list when no active categories")
    void shouldReturnEmptyListWhenNoActiveCategories() {
        // Given
        when(categoryRepository.findByIsActiveTrue()).thenReturn(Arrays.asList());

        // When
        List<CategoryResponse> responses = categoryService.getActiveCategories();

        // Then
        assertThat(responses).isEmpty();

        verify(categoryRepository).findByIsActiveTrue();
    }

    // ==================== SLUG GENERATION TESTS ====================

    @Test
    @DisplayName("Should handle special characters in slug generation")
    void shouldHandleSpecialCharactersInSlug() {
        // Given
        CreateCategoryRequest request = new CreateCategoryRequest(
                "Home & Garden!",
                "Home products",
                null,
                null,
                null
        );

        Category savedCategory = Category.builder()
                .id(1L)
                .name("Home & Garden!")
                .slug("home-garden")
                .displayOrder(0)
                .isActive(true)
                .build();

        when(categoryRepository.save(any(Category.class))).thenReturn(savedCategory);

        // When
        CategoryResponse response = categoryService.createCategory(request);

        // Then
        assertThat(response.slug())
                .doesNotContain("&", "!", " ")
                .matches("[a-z0-9-]+");
    }

    @Test
    @DisplayName("Should handle multiple spaces in slug generation")
    void shouldHandleMultipleSpacesInSlug() {
        // Given
        CreateCategoryRequest request = new CreateCategoryRequest(
                "Sports    &    Outdoors",
                "Outdoor products",
                null,
                null,
                null
        );

        Category savedCategory = Category.builder()
                .id(1L)
                .name("Sports    &    Outdoors")
                .slug("sports-outdoors")
                .displayOrder(0)
                .isActive(true)
                .build();

        when(categoryRepository.save(any(Category.class))).thenReturn(savedCategory);

        // When
        CategoryResponse response = categoryService.createCategory(request);

        // Then
        assertThat(response.slug())
                .doesNotContain("  ")
                .matches("[a-z0-9-]+");
    }

    @Test
    @DisplayName("Should handle empty repository")
    void shouldHandleEmptyRepository() {
        // Given
        when(categoryRepository.findAll()).thenReturn(Arrays.asList());

        // When
        List<CategoryResponse> responses = categoryService.getAllCategories();

        // Then
        assertThat(responses).isEmpty();
        verify(categoryRepository).findAll();
    }

    @Test
    @DisplayName("Should set default display order to 0 when not provided")
    void shouldSetDefaultDisplayOrder() {
        // Given
        CreateCategoryRequest request = new CreateCategoryRequest(
                "Test Category",
                "Test description",
                null,
                null,
                null  // displayOrder not provided
        );

        when(categoryRepository.save(any(Category.class))).thenReturn(sampleCategory);

        // When
        categoryService.createCategory(request);

        // Then
        ArgumentCaptor<Category> categoryCaptor = ArgumentCaptor.forClass(Category.class);
        verify(categoryRepository).save(categoryCaptor.capture());
        assertThat(categoryCaptor.getValue().getDisplayOrder()).isEqualTo(0);
    }
}
