package com.migracion.marketplace.catalog.dto;

import java.util.UUID;

public record CategoryResponse(UUID id, String name, String slug) {
}
