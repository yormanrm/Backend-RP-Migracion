package com.migracion.marketplace.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record RegisterAssociateRequest(
        @NotBlank @Email String email,
        @NotBlank @Size(min = 8, message = "La contraseña debe tener al menos 8 caracteres.") String password,
        @NotBlank String firstName,
        @NotBlank String lastName,
        String phone,
        @NotBlank String storeName,
        @NotBlank @Pattern(regexp = "^([A-ZÑ&]{3,4})\\d{6}([A-Z\\d]{3})$",
                flags = Pattern.Flag.CASE_INSENSITIVE,
                message = "El RFC no tiene un formato válido.") String rfc) {
}
