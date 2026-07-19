package com.migracion.marketplace.auth.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;

import com.migracion.marketplace.auth.dto.AssociateProfileResponse;
import com.migracion.marketplace.auth.dto.AssociateProfileUpdateRequest;
import com.migracion.marketplace.auth.entity.AssociateProfile;
import com.migracion.marketplace.auth.entity.User;

@Mapper(componentModel = "spring", nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
public interface AssociateProfileMapper {

    @Mapping(target = "id", source = "user.id")
    @Mapping(target = "email", source = "user.email")
    @Mapping(target = "firstName", source = "user.firstName")
    @Mapping(target = "lastName", source = "user.lastName")
    @Mapping(target = "phone", source = "user.phone")
    @Mapping(target = "storeName", source = "profile.storeName")
    @Mapping(target = "storeSlug", source = "profile.storeSlug")
    @Mapping(target = "taxId", source = "profile.taxId")
    @Mapping(target = "publicBio", source = "profile.publicBio")
    @Mapping(target = "publicContactEmail", source = "profile.publicContactEmail")
    @Mapping(target = "publicContactPhone", source = "profile.publicContactPhone")
    AssociateProfileResponse toResponse(User user, AssociateProfile profile);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "storeSlug", ignore = true)
    @Mapping(target = "user", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    void updateFromRequest(AssociateProfileUpdateRequest request, @MappingTarget AssociateProfile profile);
}
