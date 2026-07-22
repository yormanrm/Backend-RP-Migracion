package com.migracion.marketplace.catalog.dto;

import java.util.List;
import java.util.UUID;

public record CategoryResponse(UUID id, String name, List<SubcategoryResponse> subcategories) {
}
