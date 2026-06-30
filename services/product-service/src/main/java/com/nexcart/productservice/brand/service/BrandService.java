package com.nexcart.productservice.brand.service;

import com.nexcart.productservice.brand.dto.request.CreateBrandRequest;
import com.nexcart.productservice.brand.dto.response.BrandResponse;
import com.nexcart.productservice.brand.entity.Brand;
import com.nexcart.productservice.brand.repository.BrandRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class BrandService {

    private final BrandRepository brandRepository;

    @Transactional
    public BrandResponse createBrand(CreateBrandRequest request) {
        log.info("Creating brand: {}", request.name());

        Brand brand = Brand.builder()
                .name(request.name())
                .description(request.description())
                .logoUrl(request.logoUrl())
                .websiteUrl(request.websiteUrl())
                .isActive(true)
                .build();

        Brand saved = brandRepository.save(brand);
        log.info("Brand created with ID: {}", saved.getId());

        return mapToResponse(saved);
    }

    @Transactional(readOnly = true)
    public BrandResponse getBrandById(Long id) {
        log.info("Fetching brand with ID: {}", id);
        Brand brand = brandRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Brand not found with ID: " + id));
        return mapToResponse(brand);
    }

    @Transactional(readOnly = true)
    public List<BrandResponse> getAllBrands() {
        log.info("Fetching all brands");
        return brandRepository.findAll().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<BrandResponse> getActiveBrands() {
        log.info("Fetching active brands");
        return brandRepository.findByIsActiveTrue().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    private BrandResponse mapToResponse(Brand brand) {
        return BrandResponse.builder()
                .id(brand.getId())
                .name(brand.getName())
                .description(brand.getDescription())
                .logoUrl(brand.getLogoUrl())
                .websiteUrl(brand.getWebsiteUrl())
                .isActive(brand.getIsActive())
                .createdAt(brand.getCreatedAt())
                .updatedAt(brand.getUpdatedAt())
                .build();
    }
}
