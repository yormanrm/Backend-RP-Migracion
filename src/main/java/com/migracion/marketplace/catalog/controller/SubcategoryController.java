package com.migracion.marketplace.catalog.controller;

import java.util.UUID;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.migracion.marketplace.catalog.dto.SubcategoryRequest;
import com.migracion.marketplace.catalog.dto.SubcategoryResponse;
import com.migracion.marketplace.catalog.service.CategoryService;
import com.migracion.marketplace.common.dto.ApiResponse;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/subcategories")
@RequiredArgsConstructor
public class SubcategoryController {

    private final CategoryService categoryService;

    @GetMapping("/{subcategoryId}")
    public ApiResponse<SubcategoryResponse> get(@PathVariable UUID subcategoryId) {
        return ApiResponse.success(categoryService.getSubcategory(subcategoryId));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<SubcategoryResponse> create(@Valid @RequestBody SubcategoryRequest request) {
        return ApiResponse.success(categoryService.createSubcategory(request));
    }

    @PutMapping("/{subcategoryId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<SubcategoryResponse> update(@PathVariable UUID subcategoryId,
            @Valid @RequestBody SubcategoryRequest request) {
        return ApiResponse.success(categoryService.updateSubcategory(subcategoryId, request));
    }

    @DeleteMapping("/{subcategoryId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<Void> delete(@PathVariable UUID subcategoryId) {
        categoryService.deleteSubcategory(subcategoryId);
        return ApiResponse.success(null);
    }
}
