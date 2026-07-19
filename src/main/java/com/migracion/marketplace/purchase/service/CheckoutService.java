package com.migracion.marketplace.purchase.service;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.migracion.marketplace.auth.entity.User;
import com.migracion.marketplace.auth.repository.UserRepository;
import com.migracion.marketplace.catalog.entity.Item;
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
    private final OrderMapper orderMapper;

    @Transactional
    public OrderResponse checkout(UUID userId) {
        Cart cart = cartRepository.findByUserId(userId).orElse(null);
        if (cart == null || cart.getItems().isEmpty()) {
            throw new ResourceNotFoundException("El carrito está vacío.");
        }

        for (CartItem cartItem : cart.getItems()) {
            Item item = cartItem.getItem();
            if (item.getStock() < cartItem.getQuantity()) {
                throw new InsufficientStockException("Stock insuficiente para: " + item.getTitle());
            }
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado."));

        Order order = Order.builder().user(user).status(OrderStatus.CREATED).build();
        for (CartItem cartItem : cart.getItems()) {
            Item item = cartItem.getItem();
            order.getItems().add(OrderItem.builder()
                    .order(order)
                    .item(item)
                    .quantity(cartItem.getQuantity())
                    .unitPriceAtPurchase(item.getPrice())
                    .build());
            item.setStock(item.getStock() - cartItem.getQuantity());
            item.setSalesCount(item.getSalesCount() + cartItem.getQuantity());
            itemRepository.save(item);
        }
        orderRepository.save(order);

        cart.getItems().clear();
        cartRepository.save(cart);

        return orderMapper.toResponse(order);
    }

    public List<OrderResponse> listOrders(UUID userId) {
        return orderRepository.findAllByUserIdOrderByCreatedAtDesc(userId).stream()
                .map(orderMapper::toResponse)
                .toList();
    }

    public OrderResponse getOrder(UUID userId, UUID orderId) {
        Order order = orderRepository.findByIdAndUserId(orderId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Orden no encontrada."));
        return orderMapper.toResponse(order);
    }
}
