package com.migracion.marketplace.catalog.service;

import java.util.List;

import org.springframework.stereotype.Service;

import com.migracion.marketplace.catalog.dto.CategoryResponse;
import com.migracion.marketplace.catalog.mapper.CategoryMapper;
import com.migracion.marketplace.catalog.repository.CategoryRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class CategoryService {

    private final CategoryRepository categoryRepository;
    private final CategoryMapper categoryMapper;

    public List<CategoryResponse> findAll() {
        return categoryRepository.findAll().stream().map(categoryMapper::toResponse).toList();
    }
}
