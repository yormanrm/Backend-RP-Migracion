package com.migracion.marketplace.auth.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.migracion.marketplace.auth.entity.AssociateProfile;

public interface AssociateProfileRepository extends JpaRepository<AssociateProfile, UUID> {

    Optional<AssociateProfile> findByUserId(UUID userId);

    boolean existsByStoreSlug(String storeSlug);
}
