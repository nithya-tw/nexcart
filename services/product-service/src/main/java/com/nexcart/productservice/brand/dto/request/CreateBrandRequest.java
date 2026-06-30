package com.nexcart.productservice.brand.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Builder;

@Builder
public record CreateBrandRequest(
        @NotBlank(message = "Brand name is required")
        @Size(max = 100, message = "Brand name must not exceed 100 characters")
        String name,

        @Size(max = 500, message = "Description must not exceed 500 characters")
        String description,

        String logoUrl,

        String websiteUrl
) {}
