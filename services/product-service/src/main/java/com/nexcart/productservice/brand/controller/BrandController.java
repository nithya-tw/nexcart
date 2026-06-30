package com.nexcart.productservice.brand.controller;

import com.nexcart.productservice.brand.dto.request.CreateBrandRequest;
import com.nexcart.productservice.brand.dto.response.BrandResponse;
import com.nexcart.productservice.brand.service.BrandService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/v1/brands")
@RequiredArgsConstructor
public class BrandController {

    private final BrandService brandService;

    @PostMapping
    public ResponseEntity<BrandResponse> createBrand(@Valid @RequestBody CreateBrandRequest request) {
        log.info("REST request to create brand: {}", request.name());
        BrandResponse response = brandService.createBrand(request);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @GetMapping("/{id}")
    public ResponseEntity<BrandResponse> getBrandById(@PathVariable Long id) {
        log.info("REST request to get brand by ID: {}", id);
        BrandResponse response = brandService.getBrandById(id);
        return ResponseEntity.ok(response);
    }

    @GetMapping
    public ResponseEntity<List<BrandResponse>> getAllBrands() {
        log.info("REST request to get all brands");
        List<BrandResponse> response = brandService.getAllBrands();
        return ResponseEntity.ok(response);
    }

    @GetMapping("/active")
    public ResponseEntity<List<BrandResponse>> getActiveBrands() {
        log.info("REST request to get active brands");
        List<BrandResponse> response = brandService.getActiveBrands();
        return ResponseEntity.ok(response);
    }
}
