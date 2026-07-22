package com.migracion.marketplace.auth.entity;

import com.migracion.marketplace.common.entity.Auditable;

import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Lob;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
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
@Table(name = "associate_profiles")
public class AssociateProfile extends Auditable {

    @OneToOne
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @Column(nullable = false)
    private String storeName;

    @Column(nullable = false, unique = true)
    private String storeSlug;

    @Column(nullable = false, unique = true)
    private String rfc;

    @Embedded
    private Address storeAddress;

    @Lob
    private String publicBio;

    private String publicContactEmail;

    private String publicContactPhone;
}
