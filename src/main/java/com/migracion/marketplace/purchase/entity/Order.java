package com.migracion.marketplace.purchase.entity;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import com.migracion.marketplace.auth.entity.Address;
import com.migracion.marketplace.auth.entity.AssociateProfile;
import com.migracion.marketplace.auth.entity.User;
import com.migracion.marketplace.common.entity.Auditable;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Jerarquía padre/hija por auto-referencia:
 * - Padre (parentOrder == null): la compra completa. Sin status operativo (null); su estado
 *   agregado se calcula desde las hijas al responder. Guarda la dirección de envío y el total general.
 * - Hija (parentOrder != null): la porción de un asociado, con su propio status y sus OrderItem.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "orders")
public class Order extends Auditable {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = true)
    private OrderStatus status;

    @Column(precision = 12, scale = 2)
    private BigDecimal total;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_order_id")
    private Order parentOrder;

    @OneToMany(mappedBy = "parentOrder", cascade = CascadeType.ALL)
    @Builder.Default
    private List<Order> childOrders = new ArrayList<>();

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "associate_id")
    private AssociateProfile associate;

    @Embedded
    private Address shippingAddress;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<OrderItem> items = new ArrayList<>();
}
