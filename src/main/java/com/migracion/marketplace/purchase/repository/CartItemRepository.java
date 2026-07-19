package com.migracion.marketplace.purchase.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.migracion.marketplace.purchase.entity.CartItem;

public interface CartItemRepository extends JpaRepository<CartItem, UUID> {

    Optional<CartItem> findByCartIdAndItemId(UUID cartId, UUID itemId);
}
