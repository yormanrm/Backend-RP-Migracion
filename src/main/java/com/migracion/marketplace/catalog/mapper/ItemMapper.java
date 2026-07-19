package com.migracion.marketplace.catalog.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import com.migracion.marketplace.catalog.dto.ItemResponse;
import com.migracion.marketplace.catalog.dto.ItemSummaryResponse;
import com.migracion.marketplace.catalog.entity.Item;
import com.migracion.marketplace.catalog.entity.ItemImage;

@Mapper(componentModel = "spring", uses = CategoryMapper.class)
public interface ItemMapper {

    @Mapping(target = "associateInfo", source = "associate")
    ItemResponse toResponse(Item item);

    ItemSummaryResponse toSummary(Item item);

    default String map(ItemImage image) {
        return image.getUrl();
    }
}
