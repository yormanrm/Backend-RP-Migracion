package com.migracion.marketplace.catalog.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.migracion.marketplace.catalog.entity.Subcategory;

public interface SubcategoryRepository extends JpaRepository<Subcategory, UUID> {

    List<Subcategory> findByCategoryIdAndActiveTrue(UUID categoryId);
}
