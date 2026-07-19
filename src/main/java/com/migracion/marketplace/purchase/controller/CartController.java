package com.migracion.marketplace.purchase.controller;

import java.util.UUID;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.migracion.marketplace.common.dto.ApiResponse;
import com.migracion.marketplace.purchase.dto.AddToCartRequest;
import com.migracion.marketplace.purchase.dto.CartResponse;
import com.migracion.marketplace.purchase.dto.UpdateCartItemRequest;
import com.migracion.marketplace.purchase.service.CartService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/cart")
@RequiredArgsConstructor
public class CartController {

    private final CartService cartService;

    @GetMapping
    public ApiResponse<CartResponse> getCart(@AuthenticationPrincipal Jwt jwt) {
        return ApiResponse.success(cartService.getCart(userId(jwt)));
    }

    @PostMapping
    public ApiResponse<CartResponse> addItem(@AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody AddToCartRequest request) {
        return ApiResponse.success(cartService.addItem(userId(jwt), request));
    }

    @PutMapping("/items/{cartItemId}")
    public ApiResponse<CartResponse> updateItem(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID cartItemId,
            @Valid @RequestBody UpdateCartItemRequest request) {
        return ApiResponse.success(cartService.updateItem(userId(jwt), cartItemId, request));
    }

    @DeleteMapping
    public ApiResponse<CartResponse> emptyCart(@AuthenticationPrincipal Jwt jwt) {
        return ApiResponse.success(cartService.emptyCart(userId(jwt)));
    }

    private UUID userId(Jwt jwt) {
        return UUID.fromString(jwt.getSubject());
    }
}
