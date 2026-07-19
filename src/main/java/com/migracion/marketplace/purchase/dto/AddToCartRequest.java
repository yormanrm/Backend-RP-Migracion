package com.migracion.marketplace.purchase.dto;

import java.util.UUID;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record AddToCartRequest(@NotNull UUID itemId, @NotNull @Positive Integer quantity) {
}
