package com.migracion.marketplace.auth.dto;

import java.util.UUID;

public record AssociateProfileResponse(
        UUID id,
        String email,
        String firstName,
        String lastName,
        String phone,
        String storeName,
        String storeSlug,
        String taxId,
        String publicBio,
        String publicContactEmail,
        String publicContactPhone) {
}
