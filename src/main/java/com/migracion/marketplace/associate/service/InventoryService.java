package com.migracion.marketplace.associate.service;

import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.migracion.marketplace.associate.dto.ItemCreateRequest;
import com.migracion.marketplace.associate.dto.ItemUpdateRequest;
import com.migracion.marketplace.auth.entity.AssociateProfile;
import com.migracion.marketplace.auth.repository.AssociateProfileRepository;
import com.migracion.marketplace.catalog.dto.ItemResponse;
import com.migracion.marketplace.catalog.entity.Item;
import com.migracion.marketplace.catalog.entity.ItemImage;
import com.migracion.marketplace.catalog.entity.ItemType;
import com.migracion.marketplace.catalog.entity.Subcategory;
import com.migracion.marketplace.catalog.mapper.ItemMapper;
import com.migracion.marketplace.catalog.repository.ItemRepository;
import com.migracion.marketplace.catalog.repository.SubcategoryRepository;
import com.migracion.marketplace.catalog.service.BrandService;
import com.migracion.marketplace.common.exception.BusinessValidationException;
import com.migracion.marketplace.common.exception.DuplicateResourceException;
import com.migracion.marketplace.common.exception.ForbiddenOperationException;
import com.migracion.marketplace.common.exception.ResourceNotFoundException;
import com.migracion.marketplace.common.util.SlugGenerator;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class InventoryService {

    private final ItemRepository itemRepository;
    private final SubcategoryRepository subcategoryRepository;
    private final AssociateProfileRepository associateProfileRepository;
    private final BrandService brandService;
    private final ItemMapper itemMapper;
    private final SlugGenerator slugGenerator;

    @Transactional
    public ItemResponse create(UUID userId, ItemCreateRequest request) {
        AssociateProfile associate = findAssociate(userId);
        validateByType(request.type(), request.stock(), request.sku(), request.brandName());
        validateSkuUnique(associate.getId(), request.sku(), null);

        Item item = Item.builder()
                .title(request.title())
                .slug(generateItemSlug(request.title()))
                .description(request.description())
                .type(request.type())
                .price(request.price())
                .stock(request.type() == ItemType.PRODUCT ? request.stock() : null)
                .sku(request.sku())
                .model(request.model())
                .brand(resolveBrand(request.type(), request.brandName()))
                .subcategory(findSubcategory(request.subcategoryId()))
                .durationValue(serviceField(request.type(), request.durationValue()))
                .durationUnit(serviceField(request.type(), request.durationUnit()))
                .serviceMode(serviceField(request.type(), request.serviceMode()))
                .coverageZone(serviceField(request.type(), request.coverageZone()))
                .associate(associate)
                .build();
        applyImages(item, request.images());
        itemRepository.save(item);
        return itemMapper.toResponse(item);
    }

    @Transactional
    public ItemResponse update(UUID userId, UUID itemId, ItemUpdateRequest request) {
        Item item = findOwnedItem(userId, itemId);
        validateByType(request.type(), request.stock(), request.sku(), request.brandName());
        validateSkuUnique(item.getAssociate().getId(), request.sku(), itemId);

        item.setTitle(request.title());
        item.setDescription(request.description());
        item.setType(request.type());
        item.setPrice(request.price());
        item.setStock(request.type() == ItemType.PRODUCT ? request.stock() : null);
        item.setSku(request.sku());
        item.setModel(request.model());
        item.setBrand(resolveBrand(request.type(), request.brandName()));
        item.setSubcategory(findSubcategory(request.subcategoryId()));
        item.setDurationValue(serviceField(request.type(), request.durationValue()));
        item.setDurationUnit(serviceField(request.type(), request.durationUnit()));
        item.setServiceMode(serviceField(request.type(), request.serviceMode()));
        item.setCoverageZone(serviceField(request.type(), request.coverageZone()));
        item.getImages().clear();
        applyImages(item, request.images());
        itemRepository.save(item);
        return itemMapper.toResponse(item);
    }

    @Transactional
    public void delete(UUID userId, UUID itemId) {
        Item item = findOwnedItem(userId, itemId);
        item.setActive(false);
        itemRepository.save(item);
    }

    @Transactional
    public ItemResponse updateStatus(UUID userId, UUID itemId, boolean active) {
        Item item = findOwnedItem(userId, itemId);
        item.setActive(active);
        itemRepository.save(item);
        return itemMapper.toResponse(item);
    }

    public Page<ItemResponse> listOwn(UUID userId, ItemType type, Pageable pageable) {
        AssociateProfile associate = findAssociate(userId);
        Page<Item> page = type == null
                ? itemRepository.findByAssociateId(associate.getId(), pageable)
                : itemRepository.findByAssociateIdAndType(associate.getId(), type, pageable);
        return page.map(itemMapper::toResponse);
    }

    private void validateByType(ItemType type, Integer stock, String sku, String brandName) {
        if (type == ItemType.PRODUCT) {
            if (stock == null) {
                throw new BusinessValidationException("El stock es obligatorio para un producto.");
            }
            if (sku == null || sku.isBlank()) {
                throw new BusinessValidationException("El SKU es obligatorio para un producto.");
            }
            if (brandName == null || brandName.isBlank()) {
                throw new BusinessValidationException("La marca es obligatoria para un producto.");
            }
        } else if (stock != null) {
            throw new BusinessValidationException("Un servicio no debe llevar stock.");
        }
    }

    private void validateSkuUnique(UUID associateId, String sku, UUID excludeItemId) {
        if (sku == null || sku.isBlank()) {
            return;
        }
        boolean duplicated = excludeItemId == null
                ? itemRepository.existsByAssociateIdAndSku(associateId, sku)
                : itemRepository.existsByAssociateIdAndSkuAndIdNot(associateId, sku, excludeItemId);
        if (duplicated) {
            throw new DuplicateResourceException("Ya tienes un ítem con el SKU: " + sku);
        }
    }

    private com.migracion.marketplace.catalog.entity.Brand resolveBrand(ItemType type, String brandName) {
        if (brandName == null || brandName.isBlank()) {
            return null; // solo posible para SERVICE (validado antes)
        }
        return brandService.resolve(brandName);
    }

    // Los campos de servicio se ignoran (null) cuando el item es un producto.
    private <T> T serviceField(ItemType type, T value) {
        return type == ItemType.SERVICE ? value : null;
    }

    private String generateItemSlug(String title) {
        String slug = slugGenerator.toSlug(title);
        while (itemRepository.existsBySlug(slug)) {
            slug = slugGenerator.toSlug(title) + "-" + slugGenerator.randomSuffix();
        }
        return slug;
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

    private Subcategory findSubcategory(UUID subcategoryId) {
        return subcategoryRepository.findById(subcategoryId)
                .orElseThrow(() -> new ResourceNotFoundException("Subcategoría no encontrada."));
    }
}
