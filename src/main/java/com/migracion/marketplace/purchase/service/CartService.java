package com.migracion.marketplace.purchase.service;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;

import com.migracion.marketplace.auth.entity.User;
import com.migracion.marketplace.auth.repository.UserRepository;
import com.migracion.marketplace.catalog.entity.Item;
import com.migracion.marketplace.catalog.repository.ItemRepository;
import com.migracion.marketplace.common.exception.ResourceNotFoundException;
import com.migracion.marketplace.purchase.dto.AddToCartRequest;
import com.migracion.marketplace.purchase.dto.CartResponse;
import com.migracion.marketplace.purchase.dto.UpdateCartItemRequest;
import com.migracion.marketplace.purchase.entity.Cart;
import com.migracion.marketplace.purchase.entity.CartItem;
import com.migracion.marketplace.purchase.mapper.CartMapper;
import com.migracion.marketplace.purchase.repository.CartItemRepository;
import com.migracion.marketplace.purchase.repository.CartRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class CartService {

    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;
    private final ItemRepository itemRepository;
    private final UserRepository userRepository;
    private final CartMapper cartMapper;

    public CartResponse getCart(UUID userId) {
        return cartRepository.findByUserId(userId)
                .map(cartMapper::toResponse)
                .orElseGet(() -> new CartResponse(null, List.of(), BigDecimal.ZERO));
    }

    public CartResponse addItem(UUID userId, AddToCartRequest request) {
        Cart cart = findOrCreateCart(userId);
        Item item = itemRepository.findById(request.itemId())
                .orElseThrow(() -> new ResourceNotFoundException("Ítem no encontrado."));

        CartItem cartItem = cartItemRepository.findByCartIdAndItemId(cart.getId(), item.getId()).orElse(null);
        if (cartItem == null) {
            cartItem = CartItem.builder().cart(cart).item(item).quantity(request.quantity()).build();
            cart.getItems().add(cartItem);
        } else {
            cartItem.setQuantity(cartItem.getQuantity() + request.quantity());
        }
        cartRepository.save(cart);
        return cartMapper.toResponse(cart);
    }

    public CartResponse updateItem(UUID userId, UUID cartItemId, UpdateCartItemRequest request) {
        Cart cart = findOwnedCart(userId);
        CartItem cartItem = cart.getItems().stream()
                .filter(line -> line.getId().equals(cartItemId))
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("Línea de carrito no encontrada."));
        cartItem.setQuantity(request.quantity());
        cartRepository.save(cart);
        return cartMapper.toResponse(cart);
    }

    public CartResponse emptyCart(UUID userId) {
        Cart cart = findOwnedCart(userId);
        cart.getItems().clear();
        cartRepository.save(cart);
        return cartMapper.toResponse(cart);
    }

    private Cart findOrCreateCart(UUID userId) {
        return cartRepository.findByUserId(userId)
                .orElseGet(() -> {
                    User user = userRepository.findById(userId)
                            .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado."));
                    return cartRepository.save(Cart.builder().user(user).build());
                });
    }

    private Cart findOwnedCart(UUID userId) {
        return cartRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Carrito no encontrado."));
    }
}
