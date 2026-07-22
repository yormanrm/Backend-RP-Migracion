package com.migracion.marketplace.catalog.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.migracion.marketplace.catalog.entity.Category;

public interface CategoryRepository extends JpaRepository<Category, UUID> {

    List<Category> findByActiveTrueOrderByNameAsc();
}
