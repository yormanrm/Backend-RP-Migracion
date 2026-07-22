package com.migracion.marketplace.catalog.service;

import java.util.ArrayList;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import com.migracion.marketplace.catalog.dto.ItemResponse;
import com.migracion.marketplace.catalog.dto.ItemSearchRequest;
import com.migracion.marketplace.catalog.dto.ItemSummaryResponse;
import com.migracion.marketplace.catalog.entity.Item;
import com.migracion.marketplace.catalog.mapper.ItemMapper;
import com.migracion.marketplace.catalog.repository.ItemRepository;
import com.migracion.marketplace.common.exception.ResourceNotFoundException;

import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ItemQueryService {

    private final ItemRepository itemRepository;
    private final ItemMapper itemMapper;

    public Page<ItemSummaryResponse> findAllActive(Pageable pageable) {
        Specification<Item> onlyActive = (root, query, cb) -> cb.isTrue(root.get("active"));
        return itemRepository.findAll(onlyActive, pageable).map(itemMapper::toSummary);
    }

    public Page<ItemSummaryResponse> search(ItemSearchRequest request) {
        Pageable pageable = PageRequest.of(request.pageOrDefault(), request.sizeOrDefault(),
                resolveSort(request.sortByOrDefault()));
        return itemRepository.findAll(toSpecification(request), pageable).map(itemMapper::toSummary);
    }

    public ItemResponse getBySlug(String slug) {
        Item item = itemRepository.findBySlug(slug)
                .filter(Item::isActive)
                .orElseThrow(() -> new ResourceNotFoundException("Ítem no encontrado: " + slug));
        return itemMapper.toResponse(item);
    }

    private Specification<Item> toSpecification(ItemSearchRequest request) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.isTrue(root.get("active")));
            if (request.type() != null) {
                predicates.add(cb.equal(root.get("type"), request.type()));
            }
            if (request.subcategoryId() != null) {
                predicates.add(cb.equal(root.get("subcategory").get("id"), request.subcategoryId()));
            }
            if (request.brandId() != null) {
                predicates.add(cb.equal(root.get("brand").get("id"), request.brandId()));
            }
            if (request.priceMin() != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("price"), request.priceMin()));
            }
            if (request.priceMax() != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("price"), request.priceMax()));
            }
            if (request.q() != null && !request.q().isBlank()) {
                String like = "%" + request.q().trim().toLowerCase() + "%";
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("title")), like),
                        cb.like(cb.lower(root.get("slug")), like),
                        cb.like(cb.lower(root.get("model")), like),
                        cb.like(cb.lower(root.get("sku")), like),
                        cb.like(cb.lower(root.join("associate").get("storeName")), like),
                        cb.like(cb.lower(root.join("brand", JoinType.LEFT).get("name")), like)));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    private Sort resolveSort(String sortBy) {
        return switch (sortBy) {
            case "bestsellers" -> Sort.by("salesCount").descending();
            case "price_asc" -> Sort.by("price").ascending();
            case "price_desc" -> Sort.by("price").descending();
            default -> Sort.by("createdAt").descending();
        };
    }
}
