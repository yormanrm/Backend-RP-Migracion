package com.migracion.marketplace.catalog.dto;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import com.migracion.marketplace.catalog.entity.ItemType;

public record ItemSummaryResponse(
        UUID id,
        String title,
        String slug,
        ItemType type,
        BigDecimal price,
        SubcategoryResponse subcategory,
        BrandResponse brand,
        List<String> images) {
}
