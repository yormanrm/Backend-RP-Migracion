package com.migracion.marketplace.associate.controller;

import java.util.UUID;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.migracion.marketplace.associate.dto.ItemCreateRequest;
import com.migracion.marketplace.associate.dto.ItemUpdateRequest;
import com.migracion.marketplace.associate.service.InventoryService;
import com.migracion.marketplace.catalog.dto.ItemResponse;
import com.migracion.marketplace.common.dto.ApiResponse;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/associate/items")
@RequiredArgsConstructor
public class InventoryController {

    private final InventoryService inventoryService;

    @PostMapping
    @PreAuthorize("hasRole('ASSOCIATE')")
    public ApiResponse<ItemResponse> create(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody ItemCreateRequest request) {
        return ApiResponse.success(inventoryService.create(userId(jwt), request));
    }

    @PutMapping("/{itemId}")
    @PreAuthorize("hasRole('ASSOCIATE')")
    public ApiResponse<ItemResponse> update(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID itemId,
            @Valid @RequestBody ItemUpdateRequest request) {
        return ApiResponse.success(inventoryService.update(userId(jwt), itemId, request));
    }

    @DeleteMapping("/{itemId}")
    @PreAuthorize("hasRole('ASSOCIATE')")
    public ApiResponse<Void> delete(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID itemId) {
        inventoryService.delete(userId(jwt), itemId);
        return ApiResponse.success(null);
    }

    private UUID userId(Jwt jwt) {
        return UUID.fromString(jwt.getSubject());
    }
}
