package com.nexcart.productservice.category.dto.response;

import lombok.Builder;

import java.time.LocalDateTime;

@Builder
public record CategoryResponse(
        Long id,
        String name,
        String slug,
        String description,
        Long parentId,
        String imageUrl,
        Integer displayOrder,
        Boolean isActive,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {}
