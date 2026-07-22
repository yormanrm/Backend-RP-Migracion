package com.migracion.marketplace.catalog.entity;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import com.migracion.marketplace.auth.entity.AssociateProfile;
import com.migracion.marketplace.common.entity.Auditable;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Lob;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "items",
        indexes = @Index(columnList = "subcategory_id"),
        uniqueConstraints = @UniqueConstraint(columnNames = { "associate_id", "sku" }))
public class Item extends Auditable {

    @Column(nullable = false)
    private String title;

    @Column(nullable = false, unique = true)
    private String slug;

    @Lob
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ItemType type;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal price;

    // Opcional: obligatorio para PRODUCT, null para SERVICE (validado en servicio).
    private Integer stock;

    private String sku;

    private String model;

    @Column(nullable = false)
    @Builder.Default
    private Long salesCount = 0L;

    @Column(nullable = false)
    @Builder.Default
    private boolean active = true;

    // Campos solo de servicio (null para productos).
    private Integer durationValue;

    @Enumerated(EnumType.STRING)
    private DurationUnit durationUnit;

    @Enumerated(EnumType.STRING)
    private ServiceMode serviceMode;

    private String coverageZone;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "subcategory_id", nullable = false)
    private Subcategory subcategory;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "brand_id")
    private Brand brand;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "associate_id", nullable = false)
    private AssociateProfile associate;

    @OneToMany(mappedBy = "item", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<ItemImage> images = new ArrayList<>();
}
