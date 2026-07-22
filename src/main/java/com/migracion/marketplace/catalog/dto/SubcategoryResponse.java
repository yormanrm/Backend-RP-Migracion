package com.migracion.marketplace.catalog.dto;

import java.util.UUID;

public record SubcategoryResponse(
        UUID id,
        String name,
        UUID categoryId,
        String categoryName) {
}
