package com.migracion.marketplace.catalog.dto;

import java.util.UUID;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record SubcategoryRequest(
        @NotBlank String name,
        @NotNull UUID categoryId) {
}
