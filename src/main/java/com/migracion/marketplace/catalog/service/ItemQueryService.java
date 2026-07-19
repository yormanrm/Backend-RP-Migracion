package com.migracion.marketplace.catalog.service;

import java.util.ArrayList;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import com.migracion.marketplace.catalog.dto.ItemFilterCriteria;
import com.migracion.marketplace.catalog.dto.ItemResponse;
import com.migracion.marketplace.catalog.dto.ItemSummaryResponse;
import com.migracion.marketplace.catalog.entity.Item;
import com.migracion.marketplace.catalog.mapper.ItemMapper;
import com.migracion.marketplace.catalog.repository.ItemRepository;
import com.migracion.marketplace.common.exception.ResourceNotFoundException;

import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ItemQueryService {

    private final ItemRepository itemRepository;
    private final ItemMapper itemMapper;

    public Page<ItemSummaryResponse> search(ItemFilterCriteria criteria, Pageable pageable) {
        return itemRepository.findAll(toSpecification(criteria), pageable).map(itemMapper::toSummary);
    }

    public ItemResponse getBySlug(String slug) {
        Item item = itemRepository.findBySlug(slug)
                .orElseThrow(() -> new ResourceNotFoundException("Ítem no encontrado: " + slug));
        return itemMapper.toResponse(item);
    }

    private Specification<Item> toSpecification(ItemFilterCriteria criteria) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (criteria.priceMin() != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("price"), criteria.priceMin()));
            }
            if (criteria.priceMax() != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("price"), criteria.priceMax()));
            }
            if (criteria.categorySlug() != null) {
                predicates.add(cb.equal(root.get("category").get("slug"), criteria.categorySlug()));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
