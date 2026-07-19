package com.migracion.marketplace.auth.dto;

public record LoginResponse(String accessToken, String tokenType, long expiresInSeconds) {
}
