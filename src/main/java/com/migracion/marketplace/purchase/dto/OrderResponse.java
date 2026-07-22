package com.migracion.marketplace.purchase.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import com.migracion.marketplace.auth.dto.AddressDto;
import com.migracion.marketplace.purchase.entity.OrderStatus;

/** Vista del comprador: orden padre con estado agregado calculado y sus sub-órdenes por asociado. */
public record OrderResponse(
        UUID id,
        AggregateOrderStatus aggregateStatus,
        BigDecimal total,
        AddressDto shippingAddress,
        Instant createdAt,
        List<SubOrderResponse> subOrders) {

    public record SubOrderResponse(
            UUID id,
            UUID associateId,
            String storeName,
            OrderStatus status,
            BigDecimal total,
            List<OrderItemResponse> items) {
    }
}
