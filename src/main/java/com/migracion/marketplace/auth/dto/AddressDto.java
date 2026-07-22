package com.migracion.marketplace.auth.dto;

import jakarta.validation.constraints.NotBlank;

public record AddressDto(
        @NotBlank String street,
        @NotBlank String city,
        @NotBlank String state,
        @NotBlank String postalCode,
        @NotBlank String country) {
}
