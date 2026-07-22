package com.migracion.marketplace.purchase.mapper;

import java.math.BigDecimal;
import java.util.List;

import org.springframework.stereotype.Component;

import com.migracion.marketplace.auth.dto.AddressDto;
import com.migracion.marketplace.auth.entity.Address;
import com.migracion.marketplace.purchase.dto.AggregateOrderStatus;
import com.migracion.marketplace.purchase.dto.AssociateOrderResponse;
import com.migracion.marketplace.purchase.dto.OrderItemResponse;
import com.migracion.marketplace.purchase.dto.OrderResponse;
import com.migracion.marketplace.purchase.entity.Order;
import com.migracion.marketplace.purchase.entity.OrderItem;
import com.migracion.marketplace.purchase.entity.OrderStatus;

@Component
public class OrderMapper {

    /** Mapea una orden padre con sus sub-órdenes y el estado agregado calculado. */
    public OrderResponse toParentResponse(Order parent) {
        List<OrderResponse.SubOrderResponse> subOrders = parent.getChildOrders().stream()
                .map(this::toSubOrder)
                .toList();
        return new OrderResponse(parent.getId(), aggregateStatus(parent.getChildOrders()), parent.getTotal(),
                toAddressDto(parent.getShippingAddress()), parent.getCreatedAt(), subOrders);
    }

    public AssociateOrderResponse toAssociateResponse(Order child) {
        Address shipping = child.getParentOrder() == null ? null : child.getParentOrder().getShippingAddress();
        return new AssociateOrderResponse(
                child.getId(),
                child.getParentOrder() == null ? null : child.getParentOrder().getId(),
                child.getStatus(),
                child.getCreatedAt(),
                child.getTotal(),
                new AssociateOrderResponse.CustomerInfo(
                        child.getUser().getFirstName(),
                        child.getUser().getLastName(),
                        child.getUser().getEmail(),
                        child.getUser().getPhone()),
                toAddressDto(shipping),
                child.getItems().stream().map(this::toLine).toList());
    }

    private OrderResponse.SubOrderResponse toSubOrder(Order child) {
        return new OrderResponse.SubOrderResponse(
                child.getId(),
                child.getAssociate().getId(),
                child.getAssociate().getStoreName(),
                child.getStatus(),
                child.getTotal(),
                child.getItems().stream().map(this::toLine).toList());
    }

    /**
     * Estado agregado (dudastecnicas3.md punto 4): todas COMPLETED -> COMPLETADA;
     * todas CANCELLED -> CANCELADA; algunas COMPLETED -> PARCIALMENTE_COMPLETADA; resto -> EN_PROCESO.
     */
    public AggregateOrderStatus aggregateStatus(List<Order> children) {
        List<OrderStatus> statuses = children.stream().map(Order::getStatus).toList();
        if (!statuses.isEmpty() && statuses.stream().allMatch(s -> s == OrderStatus.COMPLETED)) {
            return AggregateOrderStatus.COMPLETADA;
        }
        if (!statuses.isEmpty() && statuses.stream().allMatch(s -> s == OrderStatus.CANCELLED)) {
            return AggregateOrderStatus.CANCELADA;
        }
        if (statuses.stream().anyMatch(s -> s == OrderStatus.COMPLETED)) {
            return AggregateOrderStatus.PARCIALMENTE_COMPLETADA;
        }
        return AggregateOrderStatus.EN_PROCESO;
    }

    private OrderItemResponse toLine(OrderItem orderItem) {
        BigDecimal subtotal = orderItem.getUnitPriceAtPurchase().multiply(BigDecimal.valueOf(orderItem.getQuantity()));
        return new OrderItemResponse(orderItem.getItem().getId(), orderItem.getItem().getTitle(),
                orderItem.getUnitPriceAtPurchase(), orderItem.getQuantity(), subtotal);
    }

    private AddressDto toAddressDto(Address address) {
        if (address == null) {
            return null;
        }
        return new AddressDto(address.getStreet(), address.getCity(), address.getState(),
                address.getPostalCode(), address.getCountry());
    }
}
