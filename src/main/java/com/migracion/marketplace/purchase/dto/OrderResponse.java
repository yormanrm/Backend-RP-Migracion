package com.migracion.marketplace.purchase.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import com.migracion.marketplace.purchase.entity.OrderStatus;

public record OrderResponse(UUID id, OrderStatus status, List<OrderItemResponse> items, BigDecimal total,
        Instant createdAt) {
}
