package com.migracion.marketplace.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RegisterAssociateRequest(
        @NotBlank @Email String email,
        @NotBlank @Size(min = 8, message = "La contraseña debe tener al menos 8 caracteres.") String password,
        @NotBlank String firstName,
        @NotBlank String lastName,
        String phone,
        @NotBlank String storeName,
        @NotBlank String storeSlug,
        String taxId) {
}
