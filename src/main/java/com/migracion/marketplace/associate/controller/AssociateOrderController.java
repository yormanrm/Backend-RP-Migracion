package com.migracion.marketplace.associate.controller;

import java.time.LocalDate;
import java.util.UUID;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.migracion.marketplace.associate.dto.AssociateSalesReportResponse;
import com.migracion.marketplace.associate.service.AssociateOrderService;
import com.migracion.marketplace.common.dto.ApiResponse;
import com.migracion.marketplace.common.dto.PageResponse;
import com.migracion.marketplace.purchase.dto.AssociateOrderResponse;
import com.migracion.marketplace.purchase.dto.OrderStatusUpdateRequest;
import com.migracion.marketplace.purchase.entity.OrderStatus;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/associate")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ASSOCIATE')")
public class AssociateOrderController {

    private final AssociateOrderService associateOrderService;

    @GetMapping("/orders")
    public ApiResponse<PageResponse<AssociateOrderResponse>> listOrders(@AuthenticationPrincipal Jwt jwt,
            @RequestParam(required = false) OrderStatus status,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.success(PageResponse.from(associateOrderService.listOrders(userId(jwt), status, from, to,
                PageRequest.of(page, size, Sort.by("createdAt").descending()))));
    }

    @PutMapping("/orders/{orderId}/status")
    public ApiResponse<AssociateOrderResponse> updateStatus(@AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID orderId, @Valid @RequestBody OrderStatusUpdateRequest request) {
        return ApiResponse.success(associateOrderService.updateStatus(userId(jwt), orderId, request.status()));
    }

    @GetMapping("/sales-report")
    public ApiResponse<AssociateSalesReportResponse> salesReport(@AuthenticationPrincipal Jwt jwt,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return ApiResponse.success(associateOrderService.salesReport(userId(jwt), from, to));
    }

    private UUID userId(Jwt jwt) {
        return UUID.fromString(jwt.getSubject());
    }
}
