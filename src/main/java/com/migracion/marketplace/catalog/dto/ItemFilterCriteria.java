package com.migracion.marketplace.catalog.dto;

import java.math.BigDecimal;

public record ItemFilterCriteria(BigDecimal priceMin, BigDecimal priceMax, String categorySlug) {
}
