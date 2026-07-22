package com.migracion.marketplace.purchase.repository;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.migracion.marketplace.purchase.entity.OrderItem;
import com.migracion.marketplace.purchase.entity.OrderStatus;

public interface OrderItemRepository extends JpaRepository<OrderItem, UUID> {

    @Query("""
            SELECT COALESCE(SUM(oi.unitPriceAtPurchase * oi.quantity), 0)
            FROM OrderItem oi
            WHERE oi.order.associate.id = :associateId
              AND oi.order.status = :status
              AND oi.order.createdAt >= :from AND oi.order.createdAt < :to
            """)
    BigDecimal sumRevenue(@Param("associateId") UUID associateId, @Param("status") OrderStatus status,
            @Param("from") Instant from, @Param("to") Instant to);

    @Query("""
            SELECT oi.item.id, oi.item.title, SUM(oi.quantity)
            FROM OrderItem oi
            WHERE oi.order.associate.id = :associateId
              AND oi.order.status = :status
              AND oi.order.createdAt >= :from AND oi.order.createdAt < :to
            GROUP BY oi.item.id, oi.item.title
            ORDER BY SUM(oi.quantity) DESC
            """)
    List<Object[]> salesRanking(@Param("associateId") UUID associateId, @Param("status") OrderStatus status,
            @Param("from") Instant from, @Param("to") Instant to);
}
