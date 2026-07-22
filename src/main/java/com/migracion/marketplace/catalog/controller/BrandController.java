package com.migracion.marketplace.catalog.controller;

import java.util.List;
import java.util.UUID;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.migracion.marketplace.catalog.dto.BrandResponse;
import com.migracion.marketplace.catalog.dto.BrandUpdateRequest;
import com.migracion.marketplace.catalog.service.BrandService;
import com.migracion.marketplace.common.dto.ApiResponse;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/brands")
@RequiredArgsConstructor
public class BrandController {

    private final BrandService brandService;

    @GetMapping
    public ApiResponse<List<BrandResponse>> list(@RequestParam(required = false) String q) {
        return ApiResponse.success(brandService.findAll(q));
    }

    @GetMapping("/{brandId}")
    public ApiResponse<BrandResponse> get(@PathVariable UUID brandId) {
        return ApiResponse.success(brandService.findById(brandId));
    }

    @PutMapping("/{brandId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<BrandResponse> update(@PathVariable UUID brandId,
            @Valid @RequestBody BrandUpdateRequest request) {
        return ApiResponse.success(brandService.update(brandId, request.name()));
    }

    @DeleteMapping("/{brandId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<Void> delete(@PathVariable UUID brandId) {
        brandService.delete(brandId);
        return ApiResponse.success(null);
    }
}
