package com.migracion.marketplace.purchase.controller;

import java.util.List;
import java.util.UUID;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import com.migracion.marketplace.common.dto.ApiResponse;
import com.migracion.marketplace.purchase.dto.OrderResponse;
import com.migracion.marketplace.purchase.service.CheckoutService;

import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
public class OrderController {

    private final CheckoutService checkoutService;

    @PostMapping("/checkout")
    public ApiResponse<OrderResponse> checkout(@AuthenticationPrincipal Jwt jwt) {
        return ApiResponse.success(checkoutService.checkout(userId(jwt)));
    }

    @GetMapping("/orders")
    public ApiResponse<List<OrderResponse>> listOrders(@AuthenticationPrincipal Jwt jwt) {
        return ApiResponse.success(checkoutService.listOrders(userId(jwt)));
    }

    @GetMapping("/orders/{orderId}")
    public ApiResponse<OrderResponse> getOrder(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID orderId) {
        return ApiResponse.success(checkoutService.getOrder(userId(jwt), orderId));
    }

    private UUID userId(Jwt jwt) {
        return UUID.fromString(jwt.getSubject());
    }
}
