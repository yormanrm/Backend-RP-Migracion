package com.migracion.marketplace.catalog.controller;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.migracion.marketplace.catalog.dto.ItemResponse;
import com.migracion.marketplace.catalog.dto.ItemSearchRequest;
import com.migracion.marketplace.catalog.dto.ItemSummaryResponse;
import com.migracion.marketplace.catalog.service.ItemQueryService;
import com.migracion.marketplace.common.dto.ApiResponse;
import com.migracion.marketplace.common.dto.PageResponse;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/items")
@RequiredArgsConstructor
public class ItemController {

    private final ItemQueryService itemQueryService;

    @GetMapping
    public ApiResponse<PageResponse<ItemSummaryResponse>> list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.success(PageResponse.from(
                itemQueryService.findAllActive(PageRequest.of(page, size, Sort.by("createdAt").descending()))));
    }

    @PostMapping("/search")
    public ApiResponse<PageResponse<ItemSummaryResponse>> search(@Valid @RequestBody ItemSearchRequest request) {
        return ApiResponse.success(PageResponse.from(itemQueryService.search(request)));
    }

    @GetMapping("/{slug}")
    public ApiResponse<ItemResponse> getBySlug(@PathVariable String slug) {
        return ApiResponse.success(itemQueryService.getBySlug(slug));
    }
}
