package com.migracion.marketplace.purchase.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record OrderItemResponse(UUID itemId, String title, BigDecimal unitPriceAtPurchase, Integer quantity,
        BigDecimal subtotal) {
}
