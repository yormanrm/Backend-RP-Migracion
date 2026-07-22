package com.migracion.marketplace.purchase.dto;

import com.migracion.marketplace.purchase.entity.OrderStatus;

import jakarta.validation.constraints.NotNull;

public record OrderStatusUpdateRequest(@NotNull OrderStatus status) {
}
