package com.migracion.marketplace.catalog.dto;

import java.math.BigDecimal;
import java.util.UUID;

import com.migracion.marketplace.catalog.entity.ItemType;

import jakarta.validation.constraints.PositiveOrZero;

public record ItemSearchRequest(
        String q,
        UUID subcategoryId,
        UUID brandId,
        ItemType type,
        @PositiveOrZero BigDecimal priceMin,
        @PositiveOrZero BigDecimal priceMax,
        String sortBy,
        Integer page,
        Integer size) {

    public String sortByOrDefault() {
        return sortBy == null ? "recent" : sortBy;
    }

    public int pageOrDefault() {
        return page == null ? 0 : page;
    }

    public int sizeOrDefault() {
        return size == null ? 20 : size;
    }
}
