package com.migracion.marketplace.purchase.dto;

import java.util.UUID;

/** Body opcional del checkout: sin addressId se usa la dirección predeterminada del comprador. */
public record CheckoutRequest(UUID addressId) {
}
