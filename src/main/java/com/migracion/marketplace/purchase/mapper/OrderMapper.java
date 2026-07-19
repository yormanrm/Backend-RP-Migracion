package com.migracion.marketplace.purchase.mapper;

import java.math.BigDecimal;
import java.util.List;

import org.springframework.stereotype.Component;

import com.migracion.marketplace.purchase.dto.OrderItemResponse;
import com.migracion.marketplace.purchase.dto.OrderResponse;
import com.migracion.marketplace.purchase.entity.Order;
import com.migracion.marketplace.purchase.entity.OrderItem;

@Component
public class OrderMapper {

    public OrderResponse toResponse(Order order) {
        List<OrderItemResponse> lines = order.getItems().stream().map(this::toLine).toList();
        BigDecimal total = lines.stream().map(OrderItemResponse::subtotal).reduce(BigDecimal.ZERO, BigDecimal::add);
        return new OrderResponse(order.getId(), order.getStatus(), lines, total, order.getCreatedAt());
    }

    private OrderItemResponse toLine(OrderItem orderItem) {
        BigDecimal subtotal = orderItem.getUnitPriceAtPurchase().multiply(BigDecimal.valueOf(orderItem.getQuantity()));
        return new OrderItemResponse(orderItem.getItem().getId(), orderItem.getItem().getTitle(),
                orderItem.getUnitPriceAtPurchase(), orderItem.getQuantity(), subtotal);
    }
}
