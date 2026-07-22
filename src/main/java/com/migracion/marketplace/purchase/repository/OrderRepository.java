package com.migracion.marketplace.purchase.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import com.migracion.marketplace.purchase.entity.Order;

public interface OrderRepository extends JpaRepository<Order, UUID>, JpaSpecificationExecutor<Order> {

    List<Order> findAllByUserIdAndParentOrderIsNull(UUID userId, Sort sort);

    Optional<Order> findByIdAndUserIdAndParentOrderIsNull(UUID id, UUID userId);
}
