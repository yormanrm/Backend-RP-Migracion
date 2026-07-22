package com.migracion.marketplace.catalog.service;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;

import com.migracion.marketplace.catalog.dto.BrandResponse;
import com.migracion.marketplace.catalog.entity.Brand;
import com.migracion.marketplace.catalog.repository.BrandRepository;
import com.migracion.marketplace.common.exception.ResourceNotFoundException;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class BrandService {

    private final BrandRepository brandRepository;

    public List<BrandResponse> findAll(String q) {
        List<Brand> brands = (q == null || q.isBlank())
                ? brandRepository.findByActiveTrueOrderByNameAsc()
                : brandRepository.findByNameContainingIgnoreCaseAndActiveTrueOrderByNameAsc(q.trim());
        return brands.stream().map(this::toResponse).toList();
    }

    public BrandResponse findById(UUID brandId) {
        return toResponse(findEntity(brandId));
    }

    /** Autoservicio "buscar o crear": reutiliza la marca existente (case-insensitive) o la crea. */
    public Brand resolve(String brandName) {
        String normalized = brandName.trim();
        return brandRepository.findByNameIgnoreCase(normalized)
                .orElseGet(() -> brandRepository.save(Brand.builder().name(normalized).build()));
    }

    public BrandResponse update(UUID brandId, String name) {
        Brand brand = findEntity(brandId);
        brand.setName(name.trim());
        return toResponse(brandRepository.save(brand));
    }

    public void delete(UUID brandId) {
        Brand brand = findEntity(brandId);
        brand.setActive(false);
        brandRepository.save(brand);
    }

    private Brand findEntity(UUID brandId) {
        return brandRepository.findById(brandId)
                .orElseThrow(() -> new ResourceNotFoundException("Marca no encontrada."));
    }

    private BrandResponse toResponse(Brand brand) {
        return new BrandResponse(brand.getId(), brand.getName());
    }
}
