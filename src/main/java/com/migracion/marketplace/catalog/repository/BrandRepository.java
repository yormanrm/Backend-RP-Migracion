package com.migracion.marketplace.catalog.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.migracion.marketplace.catalog.entity.Brand;

public interface BrandRepository extends JpaRepository<Brand, UUID> {

    Optional<Brand> findByNameIgnoreCase(String name);

    List<Brand> findByActiveTrueOrderByNameAsc();

    List<Brand> findByNameContainingIgnoreCaseAndActiveTrueOrderByNameAsc(String q);
}
