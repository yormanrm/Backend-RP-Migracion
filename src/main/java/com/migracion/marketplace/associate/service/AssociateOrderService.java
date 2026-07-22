package com.migracion.marketplace.associate.service;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.migracion.marketplace.associate.dto.AssociateSalesReportResponse;
import com.migracion.marketplace.auth.entity.AssociateProfile;
import com.migracion.marketplace.auth.repository.AssociateProfileRepository;
import com.migracion.marketplace.catalog.repository.ItemRepository;
import com.migracion.marketplace.common.exception.BusinessValidationException;
import com.migracion.marketplace.common.exception.ForbiddenOperationException;
import com.migracion.marketplace.common.exception.ResourceNotFoundException;
import com.migracion.marketplace.purchase.dto.AssociateOrderResponse;
import com.migracion.marketplace.purchase.entity.Order;
import com.migracion.marketplace.purchase.entity.OrderStatus;
import com.migracion.marketplace.purchase.mapper.OrderMapper;
import com.migracion.marketplace.purchase.repository.OrderItemRepository;
import com.migracion.marketplace.purchase.repository.OrderRepository;

import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AssociateOrderService {

    // Transiciones válidas de la sub-orden del asociado: PAID -> PROCESSING -> COMPLETED,
    // con CANCELLED permitido antes de COMPLETED.
    private static final Map<OrderStatus, Set<OrderStatus>> VALID_TRANSITIONS = Map.of(
            OrderStatus.PAID, Set.of(OrderStatus.PROCESSING, OrderStatus.CANCELLED),
            OrderStatus.PROCESSING, Set.of(OrderStatus.COMPLETED, OrderStatus.CANCELLED));

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final ItemRepository itemRepository;
    private final AssociateProfileRepository associateProfileRepository;
    private final OrderMapper orderMapper;

    public Page<AssociateOrderResponse> listOrders(UUID userId, OrderStatus status, LocalDate from, LocalDate to,
            Pageable pageable) {
        UUID associateId = findProfile(userId).getId();
        Specification<Order> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.equal(root.get("associate").get("id"), associateId));
            if (status != null) {
                predicates.add(cb.equal(root.get("status"), status));
            }
            if (from != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("createdAt"), toInstant(from)));
            }
            if (to != null) {
                predicates.add(cb.lessThan(root.get("createdAt"), toInstant(to.plusDays(1))));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };
        return orderRepository.findAll(spec, pageable).map(orderMapper::toAssociateResponse);
    }

    @Transactional
    public AssociateOrderResponse updateStatus(UUID userId, UUID orderId, OrderStatus newStatus) {
        UUID associateId = findProfile(userId).getId();
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Orden no encontrada."));
        if (order.getAssociate() == null || !order.getAssociate().getId().equals(associateId)) {
            throw new ForbiddenOperationException("No puedes gestionar una orden que no te pertenece.");
        }
        Set<OrderStatus> allowed = VALID_TRANSITIONS.getOrDefault(order.getStatus(), Set.of());
        if (!allowed.contains(newStatus)) {
            throw new BusinessValidationException(
                    "Transición de estado inválida: " + order.getStatus() + " -> " + newStatus);
        }
        order.setStatus(newStatus);
        orderRepository.save(order);
        return orderMapper.toAssociateResponse(order);
    }

    public AssociateSalesReportResponse salesReport(UUID userId, LocalDate from, LocalDate to) {
        UUID associateId = findProfile(userId).getId();
        Instant fromInstant = from == null ? Instant.EPOCH : toInstant(from);
        Instant toInstant = to == null ? Instant.now() : toInstant(to.plusDays(1));

        BigDecimal totalRevenue = orderItemRepository.sumRevenue(associateId, OrderStatus.COMPLETED,
                fromInstant, toInstant);
        long completedOrders = orderRepository.count((root, query, cb) -> cb.and(
                cb.equal(root.get("associate").get("id"), associateId),
                cb.equal(root.get("status"), OrderStatus.COMPLETED),
                cb.greaterThanOrEqualTo(root.get("createdAt"), fromInstant),
                cb.lessThan(root.get("createdAt"), toInstant)));

        // Ranking ordenado desc: el primero es el más vendido; el último, el menos vendido
        // entre los que sí vendieron (los de 0 ventas no aparecen en el group by).
        List<Object[]> ranking = orderItemRepository.salesRanking(associateId, OrderStatus.COMPLETED,
                fromInstant, toInstant);
        AssociateSalesReportResponse.ItemSales best = ranking.isEmpty() ? null : toItemSales(ranking.get(0));
        AssociateSalesReportResponse.ItemSales worst = ranking.isEmpty() ? null
                : toItemSales(ranking.get(ranking.size() - 1));

        long totalStock = itemRepository.sumActiveStockByAssociate(associateId);
        List<AssociateSalesReportResponse.ItemRef> zeroSales = itemRepository
                .findByAssociateIdAndSalesCount(associateId, 0L).stream()
                .map(item -> new AssociateSalesReportResponse.ItemRef(item.getId(), item.getTitle()))
                .toList();

        return new AssociateSalesReportResponse(totalRevenue, completedOrders, best, worst, totalStock, zeroSales);
    }

    private AssociateSalesReportResponse.ItemSales toItemSales(Object[] row) {
        return new AssociateSalesReportResponse.ItemSales((UUID) row[0], (String) row[1],
                ((Number) row[2]).longValue());
    }

    private Instant toInstant(LocalDate date) {
        return date.atStartOfDay(ZoneOffset.UTC).toInstant();
    }

    private AssociateProfile findProfile(UUID userId) {
        return associateProfileRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Perfil de asociado no encontrado."));
    }
}
