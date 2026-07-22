package com.migracion.marketplace.auth.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.migracion.marketplace.auth.entity.UserAddress;

public interface UserAddressRepository extends JpaRepository<UserAddress, UUID> {

    List<UserAddress> findByUserIdOrderByCreatedAtAsc(UUID userId);

    Optional<UserAddress> findByIdAndUserId(UUID id, UUID userId);

    Optional<UserAddress> findByUserIdAndIsDefaultTrue(UUID userId);

    boolean existsByUserId(UUID userId);
}
