package com.migracion.marketplace.auth.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;

public record AssociateProfileUpdateRequest(
        @NotBlank String firstName,
        @NotBlank String lastName,
        String phone,
        @NotBlank String storeName,
        @Valid AddressDto storeAddress,
        String publicBio,
        String publicContactEmail,
        String publicContactPhone) {
}
