package com.nexcart.productservice.product.dto.request;

import jakarta.validation.constraints.*;
import lombok.Builder;

import java.math.BigDecimal;
import java.util.Map;

@Builder
public record CreateProductRequest(
        @NotBlank(message = "SKU is required")
        @Size(max = 100, message = "SKU must not exceed 100 characters")
        String sku,

        @NotBlank(message = "Product name is required")
        @Size(max = 255, message = "Product name must not exceed 255 characters")
        String name,

        @Size(max = 500, message = "Short description must not exceed 500 characters")
        String shortDescription,

        String description,

        @NotNull(message = "Price is required")
        @DecimalMin(value = "0.0", inclusive = true, message = "Price must be positive")
        BigDecimal price,

        @DecimalMin(value = "0.0", inclusive = true, message = "Compare price must be positive")
        BigDecimal compareAtPrice,

        @DecimalMin(value = "0.0", inclusive = true, message = "Cost price must be positive")
        BigDecimal costPrice,

        @NotNull(message = "Brand ID is required")
        Long brandId,

        @NotNull(message = "Category ID is required")
        Long categoryId,

        Map<String, Object> attributes,

        @Size(max = 200)
        String metaTitle,

        @Size(max = 500)
        String metaDescription,

        Boolean isFeatured
) {}
