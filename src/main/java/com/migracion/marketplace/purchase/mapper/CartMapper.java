package com.migracion.marketplace.purchase.mapper;

import java.math.BigDecimal;
import java.util.List;

import org.springframework.stereotype.Component;

import com.migracion.marketplace.purchase.dto.CartResponse;
import com.migracion.marketplace.purchase.dto.CartResponse.CartLineResponse;
import com.migracion.marketplace.purchase.entity.Cart;
import com.migracion.marketplace.purchase.entity.CartItem;

@Component
public class CartMapper {

    public CartResponse toResponse(Cart cart) {
        List<CartLineResponse> lines = cart.getItems().stream().map(this::toLine).toList();
        BigDecimal total = lines.stream().map(CartLineResponse::subtotal).reduce(BigDecimal.ZERO, BigDecimal::add);
        return new CartResponse(cart.getId(), lines, total);
    }

    private CartLineResponse toLine(CartItem cartItem) {
        BigDecimal subtotal = cartItem.getItem().getPrice().multiply(BigDecimal.valueOf(cartItem.getQuantity()));
        return new CartLineResponse(cartItem.getId(), cartItem.getItem().getId(), cartItem.getItem().getTitle(),
                cartItem.getItem().getPrice(), cartItem.getQuantity(), subtotal);
    }
}
