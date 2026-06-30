package com.nexcart.productservice.category.service;

import com.nexcart.productservice.category.dto.request.CreateCategoryRequest;
import com.nexcart.productservice.category.dto.response.CategoryResponse;
import com.nexcart.productservice.category.entity.Category;
import com.nexcart.productservice.category.repository.CategoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class CategoryService {

    private final CategoryRepository categoryRepository;

    @Transactional
    public CategoryResponse createCategory(CreateCategoryRequest request) {
        log.info("Creating category: {}", request.name());

        Category category = Category.builder()
                .name(request.name())
                .slug(generateSlug(request.name()))
                .description(request.description())
                .parentId(request.parentId())
                .imageUrl(request.imageUrl())
                .displayOrder(request.displayOrder() != null ? request.displayOrder() : 0)
                .isActive(true)
                .build();

        Category saved = categoryRepository.save(category);
        log.info("Category created with ID: {}", saved.getId());

        return mapToResponse(saved);
    }

    @Transactional(readOnly = true)
    public CategoryResponse getCategoryById(Long id) {
        log.info("Fetching category with ID: {}", id);
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Category not found with ID: " + id));
        return mapToResponse(category);
    }

    @Transactional(readOnly = true)
    public List<CategoryResponse> getAllCategories() {
        log.info("Fetching all categories");
        return categoryRepository.findAll().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<CategoryResponse> getActiveCategories() {
        log.info("Fetching active categories");
        return categoryRepository.findByIsActiveTrue().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    private String generateSlug(String name) {
        return name.toLowerCase()
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("^-|-$", "");
    }

    private CategoryResponse mapToResponse(Category category) {
        return CategoryResponse.builder()
                .id(category.getId())
                .name(category.getName())
                .slug(category.getSlug())
                .description(category.getDescription())
                .parentId(category.getParentId())
                .imageUrl(category.getImageUrl())
                .displayOrder(category.getDisplayOrder())
                .isActive(category.getIsActive())
                .createdAt(category.getCreatedAt())
                .updatedAt(category.getUpdatedAt())
                .build();
    }
}
