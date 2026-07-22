package com.migracion.marketplace.auth.dto;

import java.util.UUID;

public record CustomerProfileResponse(
        UUID id,
        String email,
        String firstName,
        String lastName,
        String phone) {
}
