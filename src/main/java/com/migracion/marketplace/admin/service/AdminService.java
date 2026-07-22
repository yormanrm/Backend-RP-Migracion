package com.migracion.marketplace.admin.service;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.migracion.marketplace.admin.dto.AdminUserSummaryResponse;
import com.migracion.marketplace.auth.entity.Role;
import com.migracion.marketplace.auth.entity.User;
import com.migracion.marketplace.auth.repository.AssociateProfileRepository;
import com.migracion.marketplace.auth.repository.UserRepository;
import com.migracion.marketplace.catalog.dto.ItemResponse;
import com.migracion.marketplace.catalog.entity.Item;
import com.migracion.marketplace.catalog.entity.ItemType;
import com.migracion.marketplace.catalog.mapper.ItemMapper;
import com.migracion.marketplace.catalog.repository.ItemRepository;
import com.migracion.marketplace.common.exception.ResourceNotFoundException;

import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AdminService {

    private final UserRepository userRepository;
    private final AssociateProfileRepository associateProfileRepository;
    private final ItemRepository itemRepository;
    private final ItemMapper itemMapper;

    public Page<AdminUserSummaryResponse> listUsers(Pageable pageable) {
        return userRepository.findAll(pageable).map(user -> new AdminUserSummaryResponse(
                user.getId(), user.getEmail(), user.getFirstName(), user.getLastName(),
                user.getRole(), user.isActive(), user.getCreatedAt()));
    }

    @Transactional
    public void updateUserStatus(UUID userId, boolean active) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado."));
        user.setActive(active);
        userRepository.save(user);
        // Cascada: al desactivar un asociado, todos sus items pasan a inactivos.
        // La reactivación NO reactiva items (decisión: reactivación manual por el asociado).
        if (!active && user.getRole() == Role.ASSOCIATE) {
            associateProfileRepository.findByUserId(userId)
                    .ifPresent(profile -> itemRepository.deactivateAllByAssociate(profile.getId()));
        }
    }

    @Transactional
    public void updateItemStatus(UUID itemId, boolean active) {
        Item item = itemRepository.findById(itemId)
                .orElseThrow(() -> new ResourceNotFoundException("Ítem no encontrado."));
        item.setActive(active);
        itemRepository.save(item);
    }

    public Page<ItemResponse> listItems(ItemType type, Boolean active, Pageable pageable) {
        Specification<Item> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (type != null) {
                predicates.add(cb.equal(root.get("type"), type));
            }
            if (active != null) {
                predicates.add(cb.equal(root.get("active"), active));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };
        return itemRepository.findAll(spec, pageable).map(itemMapper::toResponse);
    }
}
