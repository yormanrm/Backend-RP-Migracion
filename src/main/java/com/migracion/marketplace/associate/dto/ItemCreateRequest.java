package com.migracion.marketplace.associate.dto;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import com.migracion.marketplace.catalog.entity.DurationUnit;
import com.migracion.marketplace.catalog.entity.ItemType;
import com.migracion.marketplace.catalog.entity.ServiceMode;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;

public record ItemCreateRequest(
        @NotBlank String title,
        String description,
        @NotNull ItemType type,
        @NotNull @DecimalMin(value = "0.0", inclusive = false) BigDecimal price,
        @PositiveOrZero Integer stock,
        String sku,
        String model,
        String brandName,
        @NotNull UUID subcategoryId,
        @Positive Integer durationValue,
        DurationUnit durationUnit,
        ServiceMode serviceMode,
        String coverageZone,
        List<String> images) {
}
