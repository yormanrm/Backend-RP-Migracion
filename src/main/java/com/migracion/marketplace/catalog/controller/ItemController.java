package com.migracion.marketplace.catalog.controller;

import java.math.BigDecimal;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.migracion.marketplace.catalog.dto.ItemFilterCriteria;
import com.migracion.marketplace.catalog.dto.ItemResponse;
import com.migracion.marketplace.catalog.dto.ItemSummaryResponse;
import com.migracion.marketplace.catalog.service.ItemQueryService;
import com.migracion.marketplace.common.dto.ApiResponse;
import com.migracion.marketplace.common.dto.PageResponse;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/items")
@RequiredArgsConstructor
public class ItemController {

    private final ItemQueryService itemQueryService;

    @GetMapping
    public ApiResponse<PageResponse<ItemSummaryResponse>> search(
            @RequestParam(required = false) BigDecimal priceMin,
            @RequestParam(required = false) BigDecimal priceMax,
            @RequestParam(required = false) String categorySlug,
            @RequestParam(defaultValue = "recent") String sortBy,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        ItemFilterCriteria criteria = new ItemFilterCriteria(priceMin, priceMax, categorySlug);
        Pageable pageable = PageRequest.of(page, size, resolveSort(sortBy));
        return ApiResponse.success(PageResponse.from(itemQueryService.search(criteria, pageable)));
    }

    @GetMapping("/{slug}")
    public ApiResponse<ItemResponse> getBySlug(@PathVariable String slug) {
        return ApiResponse.success(itemQueryService.getBySlug(slug));
    }

    private Sort resolveSort(String sortBy) {
        return switch (sortBy) {
            case "bestsellers" -> Sort.by("salesCount").descending();
            case "price_asc" -> Sort.by("price").ascending();
            case "price_desc" -> Sort.by("price").descending();
            default -> Sort.by("createdAt").descending();
        };
    }
}
