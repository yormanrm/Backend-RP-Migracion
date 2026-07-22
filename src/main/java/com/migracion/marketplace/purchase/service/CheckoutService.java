package com.migracion.marketplace.purchase.service;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.migracion.marketplace.auth.entity.Address;
import com.migracion.marketplace.auth.entity.AssociateProfile;
import com.migracion.marketplace.auth.entity.User;
import com.migracion.marketplace.auth.entity.UserAddress;
import com.migracion.marketplace.auth.repository.UserRepository;
import com.migracion.marketplace.auth.service.UserAddressService;
import com.migracion.marketplace.catalog.entity.Item;
import com.migracion.marketplace.catalog.entity.ItemType;
import com.migracion.marketplace.catalog.repository.ItemRepository;
import com.migracion.marketplace.common.exception.InsufficientStockException;
import com.migracion.marketplace.common.exception.ResourceNotFoundException;
import com.migracion.marketplace.purchase.dto.OrderResponse;
import com.migracion.marketplace.purchase.entity.Cart;
import com.migracion.marketplace.purchase.entity.CartItem;
import com.migracion.marketplace.purchase.entity.Order;
import com.migracion.marketplace.purchase.entity.OrderItem;
import com.migracion.marketplace.purchase.entity.OrderStatus;
import com.migracion.marketplace.purchase.mapper.OrderMapper;
import com.migracion.marketplace.purchase.repository.CartRepository;
import com.migracion.marketplace.purchase.repository.OrderRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class CheckoutService {

    private final CartRepository cartRepository;
    private final ItemRepository itemRepository;
    private final UserRepository userRepository;
    private final OrderRepository orderRepository;
    private final UserAddressService userAddressService;
    private final OrderMapper orderMapper;

    @Transactional
    public OrderResponse checkout(UUID userId, UUID addressId) {
        Cart cart = cartRepository.findByUserId(userId).orElse(null);
        if (cart == null || cart.getItems().isEmpty()) {
            throw new ResourceNotFoundException("El carrito está vacío.");
        }

        for (CartItem cartItem : cart.getItems()) {
            Item item = cartItem.getItem();
            if (item.getType() == ItemType.PRODUCT && item.getStock() < cartItem.getQuantity()) {
                throw new InsufficientStockException("Stock insuficiente para: " + item.getTitle());
            }
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado."));

        UserAddress shipping = userAddressService.resolveForCheckout(userId, addressId);

        // Orden padre: la compra completa. Sin status operativo; snapshot de la dirección de envío.
        Order parent = Order.builder()
                .user(user)
                .shippingAddress(shipping == null ? null
                        : new Address(shipping.getStreet(), shipping.getCity(), shipping.getState(),
                                shipping.getPostalCode(), shipping.getCountry()))
                .build();

        // Agrupar líneas del carrito por asociado y crear una sub-orden por cada uno.
        Map<AssociateProfile, List<CartItem>> byAssociate = new LinkedHashMap<>();
        for (CartItem cartItem : cart.getItems()) {
            byAssociate.computeIfAbsent(cartItem.getItem().getAssociate(), key -> new java.util.ArrayList<>())
                    .add(cartItem);
        }

        BigDecimal grandTotal = BigDecimal.ZERO;
        for (Map.Entry<AssociateProfile, List<CartItem>> entry : byAssociate.entrySet()) {
            Order child = Order.builder()
                    .user(user)
                    .associate(entry.getKey())
                    .status(OrderStatus.PAID)
                    .parentOrder(parent)
                    .build();
            BigDecimal childTotal = BigDecimal.ZERO;
            for (CartItem cartItem : entry.getValue()) {
                Item item = cartItem.getItem();
                child.getItems().add(OrderItem.builder()
                        .order(child)
                        .item(item)
                        .quantity(cartItem.getQuantity())
                        .unitPriceAtPurchase(item.getPrice())
                        .build());
                childTotal = childTotal.add(item.getPrice().multiply(BigDecimal.valueOf(cartItem.getQuantity())));
                if (item.getType() == ItemType.PRODUCT) {
                    item.setStock(item.getStock() - cartItem.getQuantity());
                }
                item.setSalesCount(item.getSalesCount() + cartItem.getQuantity());
                itemRepository.save(item);
            }
            child.setTotal(childTotal);
            grandTotal = grandTotal.add(childTotal);
            parent.getChildOrders().add(child);
        }
        parent.setTotal(grandTotal);
        orderRepository.save(parent);

        cart.getItems().clear();
        cartRepository.save(cart);

        return orderMapper.toParentResponse(parent);
    }

    public List<OrderResponse> listOrders(UUID userId, String sort) {
        Sort order = "oldest".equals(sort)
                ? Sort.by("createdAt").ascending()
                : Sort.by("createdAt").descending();
        return orderRepository.findAllByUserIdAndParentOrderIsNull(userId, order).stream()
                .map(orderMapper::toParentResponse)
                .toList();
    }

    public OrderResponse getOrder(UUID userId, UUID orderId) {
        Order order = orderRepository.findByIdAndUserIdAndParentOrderIsNull(orderId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Orden no encontrada."));
        return orderMapper.toParentResponse(order);
    }
}
