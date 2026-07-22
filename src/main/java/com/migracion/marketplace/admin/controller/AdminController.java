package com.migracion.marketplace.admin.controller;

import java.util.UUID;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.migracion.marketplace.admin.dto.AdminUserSummaryResponse;
import com.migracion.marketplace.admin.service.AdminService;
import com.migracion.marketplace.catalog.dto.ItemResponse;
import com.migracion.marketplace.catalog.entity.ItemType;
import com.migracion.marketplace.common.dto.ApiResponse;
import com.migracion.marketplace.common.dto.PageResponse;
import com.migracion.marketplace.common.dto.StatusUpdateRequest;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/admin")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {

    private final AdminService adminService;

    @GetMapping("/users")
    public ApiResponse<PageResponse<AdminUserSummaryResponse>> listUsers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.success(PageResponse.from(
                adminService.listUsers(PageRequest.of(page, size, Sort.by("createdAt").descending()))));
    }

    @PutMapping("/users/{userId}/status")
    public ApiResponse<Void> updateUserStatus(@PathVariable UUID userId,
            @Valid @RequestBody StatusUpdateRequest request) {
        adminService.updateUserStatus(userId, request.active());
        return ApiResponse.success(null);
    }

    @PutMapping("/items/{itemId}/status")
    public ApiResponse<Void> updateItemStatus(@PathVariable UUID itemId,
            @Valid @RequestBody StatusUpdateRequest request) {
        adminService.updateItemStatus(itemId, request.active());
        return ApiResponse.success(null);
    }

    @GetMapping("/items")
    public ApiResponse<PageResponse<ItemResponse>> listItems(
            @RequestParam(required = false) ItemType type,
            @RequestParam(required = false) Boolean active,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.success(PageResponse.from(adminService.listItems(type, active,
                PageRequest.of(page, size, Sort.by("createdAt").descending()))));
    }
}
