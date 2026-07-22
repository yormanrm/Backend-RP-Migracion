package com.migracion.marketplace.purchase.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import com.migracion.marketplace.auth.dto.AddressDto;
import com.migracion.marketplace.purchase.entity.OrderStatus;

/** Vista del asociado: su sub-orden, con datos no sensibles del comprador. */
public record AssociateOrderResponse(
        UUID id,
        UUID parentOrderId,
        OrderStatus status,
        Instant createdAt,
        BigDecimal total,
        CustomerInfo customer,
        AddressDto shippingAddress,
        List<OrderItemResponse> items) {

    public record CustomerInfo(String firstName, String lastName, String email, String phone) {
    }
}
