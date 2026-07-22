package com.migracion.marketplace.catalog.dto;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import com.migracion.marketplace.catalog.entity.DurationUnit;
import com.migracion.marketplace.catalog.entity.ItemType;
import com.migracion.marketplace.catalog.entity.ServiceMode;

public record ItemResponse(
        UUID id,
        String title,
        String slug,
        ItemType type,
        BigDecimal price,
        Integer stock,
        String sku,
        String model,
        String description,
        boolean active,
        SubcategoryResponse subcategory,
        BrandResponse brand,
        Integer durationValue,
        DurationUnit durationUnit,
        ServiceMode serviceMode,
        String coverageZone,
        AssociateInfoResponse associateInfo,
        List<String> images) {
}
