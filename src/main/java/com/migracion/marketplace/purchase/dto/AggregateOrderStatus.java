package com.migracion.marketplace.purchase.dto;

/** Estado agregado de la orden padre. Calculado en la respuesta, nunca persistido. */
public enum AggregateOrderStatus {
    EN_PROCESO,
    PARCIALMENTE_COMPLETADA,
    COMPLETADA,
    CANCELADA
}
