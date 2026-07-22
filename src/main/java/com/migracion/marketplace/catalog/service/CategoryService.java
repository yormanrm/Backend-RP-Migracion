package com.migracion.marketplace.catalog.service;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;

import com.migracion.marketplace.catalog.dto.CategoryRequest;
import com.migracion.marketplace.catalog.dto.CategoryResponse;
import com.migracion.marketplace.catalog.dto.SubcategoryRequest;
import com.migracion.marketplace.catalog.dto.SubcategoryResponse;
import com.migracion.marketplace.catalog.entity.Category;
import com.migracion.marketplace.catalog.entity.Subcategory;
import com.migracion.marketplace.catalog.mapper.CategoryMapper;
import com.migracion.marketplace.catalog.repository.CategoryRepository;
import com.migracion.marketplace.catalog.repository.SubcategoryRepository;
import com.migracion.marketplace.common.exception.ResourceNotFoundException;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class CategoryService {

    private final CategoryRepository categoryRepository;
    private final SubcategoryRepository subcategoryRepository;
    private final CategoryMapper categoryMapper;

    public List<CategoryResponse> findAll() {
        return categoryRepository.findByActiveTrueOrderByNameAsc().stream()
                .map(category -> new CategoryResponse(
                        category.getId(),
                        category.getName(),
                        subcategoryRepository.findByCategoryIdAndActiveTrue(category.getId()).stream()
                                .map(categoryMapper::toSubcategoryResponse)
                                .toList()))
                .toList();
    }

    public SubcategoryResponse getSubcategory(UUID subcategoryId) {
        return categoryMapper.toSubcategoryResponse(findSubcategoryEntity(subcategoryId));
    }

    public CategoryResponse createCategory(CategoryRequest request) {
        Category category = categoryRepository.save(Category.builder().name(request.name()).build());
        return new CategoryResponse(category.getId(), category.getName(), List.of());
    }

    public CategoryResponse updateCategory(UUID categoryId, CategoryRequest request) {
        Category category = findCategoryEntity(categoryId);
        category.setName(request.name());
        categoryRepository.save(category);
        return new CategoryResponse(category.getId(), category.getName(),
                subcategoryRepository.findByCategoryIdAndActiveTrue(categoryId).stream()
                        .map(categoryMapper::toSubcategoryResponse)
                        .toList());
    }

    public void deleteCategory(UUID categoryId) {
        Category category = findCategoryEntity(categoryId);
        category.setActive(false);
        categoryRepository.save(category);
    }

    public SubcategoryResponse createSubcategory(SubcategoryRequest request) {
        Subcategory subcategory = Subcategory.builder()
                .name(request.name())
                .category(findCategoryEntity(request.categoryId()))
                .build();
        return categoryMapper.toSubcategoryResponse(subcategoryRepository.save(subcategory));
    }

    public SubcategoryResponse updateSubcategory(UUID subcategoryId, SubcategoryRequest request) {
        Subcategory subcategory = findSubcategoryEntity(subcategoryId);
        subcategory.setName(request.name());
        subcategory.setCategory(findCategoryEntity(request.categoryId()));
        return categoryMapper.toSubcategoryResponse(subcategoryRepository.save(subcategory));
    }

    public void deleteSubcategory(UUID subcategoryId) {
        Subcategory subcategory = findSubcategoryEntity(subcategoryId);
        subcategory.setActive(false);
        subcategoryRepository.save(subcategory);
    }

    private Category findCategoryEntity(UUID categoryId) {
        return categoryRepository.findById(categoryId)
                .orElseThrow(() -> new ResourceNotFoundException("Categoría no encontrada."));
    }

    private Subcategory findSubcategoryEntity(UUID subcategoryId) {
        return subcategoryRepository.findById(subcategoryId)
                .orElseThrow(() -> new ResourceNotFoundException("Subcategoría no encontrada."));
    }
}
