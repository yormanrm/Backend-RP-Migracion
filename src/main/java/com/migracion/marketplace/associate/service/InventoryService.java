package com.migracion.marketplace.associate.service;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;

import com.migracion.marketplace.associate.dto.ItemCreateRequest;
import com.migracion.marketplace.associate.dto.ItemUpdateRequest;
import com.migracion.marketplace.auth.entity.AssociateProfile;
import com.migracion.marketplace.auth.repository.AssociateProfileRepository;
import com.migracion.marketplace.catalog.dto.ItemResponse;
import com.migracion.marketplace.catalog.entity.Category;
import com.migracion.marketplace.catalog.entity.Item;
import com.migracion.marketplace.catalog.entity.ItemImage;
import com.migracion.marketplace.catalog.mapper.ItemMapper;
import com.migracion.marketplace.catalog.repository.CategoryRepository;
import com.migracion.marketplace.catalog.repository.ItemRepository;
import com.migracion.marketplace.common.exception.DuplicateResourceException;
import com.migracion.marketplace.common.exception.ForbiddenOperationException;
import com.migracion.marketplace.common.exception.ResourceNotFoundException;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class InventoryService {

    private final ItemRepository itemRepository;
    private final CategoryRepository categoryRepository;
    private final AssociateProfileRepository associateProfileRepository;
    private final ItemMapper itemMapper;

    public ItemResponse create(UUID userId, ItemCreateRequest request) {
        AssociateProfile associate = findAssociate(userId);
        if (itemRepository.existsBySlug(request.slug())) {
            throw new DuplicateResourceException("Ya existe un ítem con el slug: " + request.slug());
        }
        Item item = Item.builder()
                .title(request.title())
                .slug(request.slug())
                .description(request.description())
                .price(request.price())
                .stock(request.stock())
                .category(findCategory(request.categoryId()))
                .associate(associate)
                .build();
        applyImages(item, request.images());
        itemRepository.save(item);
        return itemMapper.toResponse(item);
    }

    public ItemResponse update(UUID userId, UUID itemId, ItemUpdateRequest request) {
        Item item = findOwnedItem(userId, itemId);
        if (!item.getSlug().equals(request.slug()) && itemRepository.existsBySlug(request.slug())) {
            throw new DuplicateResourceException("Ya existe un ítem con el slug: " + request.slug());
        }
        item.setTitle(request.title());
        item.setSlug(request.slug());
        item.setDescription(request.description());
        item.setPrice(request.price());
        item.setStock(request.stock());
        item.setCategory(findCategory(request.categoryId()));
        item.getImages().clear();
        applyImages(item, request.images());
        itemRepository.save(item);
        return itemMapper.toResponse(item);
    }

    public void delete(UUID userId, UUID itemId) {
        itemRepository.delete(findOwnedItem(userId, itemId));
    }

    private Item findOwnedItem(UUID userId, UUID itemId) {
        Item item = itemRepository.findById(itemId)
                .orElseThrow(() -> new ResourceNotFoundException("Ítem no encontrado."));
        AssociateProfile associate = findAssociate(userId);
        if (!item.getAssociate().getId().equals(associate.getId())) {
            throw new ForbiddenOperationException("No puedes modificar un ítem que no te pertenece.");
        }
        return item;
    }

    private void applyImages(Item item, List<String> urls) {
        if (urls == null) {
            return;
        }
        urls.forEach(url -> item.getImages().add(ItemImage.builder().url(url).item(item).build()));
    }

    private AssociateProfile findAssociate(UUID userId) {
        return associateProfileRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Perfil de asociado no encontrado."));
    }

    private Category findCategory(UUID categoryId) {
        return categoryRepository.findById(categoryId)
                .orElseThrow(() -> new ResourceNotFoundException("Categoría no encontrada."));
    }
}
