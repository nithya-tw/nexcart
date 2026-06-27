package com.nexcart.productservice.product.dto.response;

import lombok.Builder;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;

@Builder
public record ProductResponse(
        Long id,
        String sku,
        String name,
        String slug,
        String description,
        String shortDescription,
        BigDecimal price,
        BigDecimal compareAtPrice,
        BigDecimal costPrice,
        Long brandId,
        Long categoryId,
        Map<String, Object> attributes,
        String metaTitle,
        String metaDescription,
        Boolean isActive,
        Boolean isFeatured,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {}
