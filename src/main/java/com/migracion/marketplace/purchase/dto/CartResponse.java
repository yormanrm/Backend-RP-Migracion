package com.migracion.marketplace.purchase.dto;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record CartResponse(UUID id, List<CartLineResponse> items, BigDecimal total) {

    public record CartLineResponse(UUID cartItemId, UUID itemId, String title, BigDecimal price, Integer quantity,
            BigDecimal subtotal) {
    }
}
