package com.migracion.marketplace.common.dto;

import jakarta.validation.constraints.NotNull;

public record StatusUpdateRequest(@NotNull Boolean active) {
}
