package com.nexcart.productservice.brand.dto.response;

import lombok.Builder;

import java.time.LocalDateTime;

@Builder
public record BrandResponse(
        Long id,
        String name,
        String description,
        String logoUrl,
        String websiteUrl,
        Boolean isActive,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {}
