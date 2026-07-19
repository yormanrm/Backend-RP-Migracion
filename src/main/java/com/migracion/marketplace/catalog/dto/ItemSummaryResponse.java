package com.migracion.marketplace.catalog.dto;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record ItemSummaryResponse(
        UUID id,
        String title,
        String slug,
        BigDecimal price,
        CategoryResponse category,
        List<String> images) {
}
