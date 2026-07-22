package com.migracion.marketplace.catalog.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.migracion.marketplace.catalog.entity.Item;
import com.migracion.marketplace.catalog.entity.ItemType;

public interface ItemRepository extends JpaRepository<Item, UUID>, JpaSpecificationExecutor<Item> {

    Optional<Item> findBySlug(String slug);

    boolean existsBySlug(String slug);

    boolean existsByAssociateIdAndSku(UUID associateId, String sku);

    boolean existsByAssociateIdAndSkuAndIdNot(UUID associateId, String sku, UUID itemId);

    Page<Item> findByAssociateId(UUID associateId, Pageable pageable);

    Page<Item> findByAssociateIdAndType(UUID associateId, ItemType type, Pageable pageable);

    @Modifying
    @Query("UPDATE Item i SET i.active = false WHERE i.associate.id = :associateId")
    void deactivateAllByAssociate(@Param("associateId") UUID associateId);

    @Query("SELECT COALESCE(SUM(i.stock), 0) FROM Item i WHERE i.associate.id = :associateId AND i.active = true AND i.stock IS NOT NULL")
    long sumActiveStockByAssociate(@Param("associateId") UUID associateId);

    java.util.List<Item> findByAssociateIdAndSalesCount(UUID associateId, Long salesCount);
}
