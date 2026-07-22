package com.migracion.marketplace.admin.dto;

import java.time.Instant;
import java.util.UUID;

import com.migracion.marketplace.auth.entity.Role;

public record AdminUserSummaryResponse(
        UUID id,
        String email,
        String firstName,
        String lastName,
        Role role,
        boolean active,
        Instant createdAt) {
}
