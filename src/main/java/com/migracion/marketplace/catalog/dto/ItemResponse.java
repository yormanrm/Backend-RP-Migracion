package com.migracion.marketplace.catalog.dto;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record ItemResponse(
        UUID id,
        String title,
        String slug,
        BigDecimal price,
        String description,
        CategoryResponse category,
        AssociateInfoResponse associateInfo,
        List<String> images) {
}
