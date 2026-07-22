package com.migracion.marketplace.auth.dto;

import java.util.UUID;

public record UserAddressResponse(
        UUID id,
        String street,
        String city,
        String state,
        String postalCode,
        String country,
        boolean isDefault) {
}
