package com.migracion.marketplace.catalog.mapper;

import org.mapstruct.Mapper;

import com.migracion.marketplace.catalog.dto.CategoryResponse;
import com.migracion.marketplace.catalog.entity.Category;

@Mapper(componentModel = "spring")
public interface CategoryMapper {

    CategoryResponse toResponse(Category category);
}
