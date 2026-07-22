package com.migracion.marketplace.catalog.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import com.migracion.marketplace.catalog.dto.SubcategoryResponse;
import com.migracion.marketplace.catalog.entity.Subcategory;

@Mapper(componentModel = "spring")
public interface CategoryMapper {

    @Mapping(target = "categoryId", source = "category.id")
    @Mapping(target = "categoryName", source = "category.name")
    SubcategoryResponse toSubcategoryResponse(Subcategory subcategory);
}
