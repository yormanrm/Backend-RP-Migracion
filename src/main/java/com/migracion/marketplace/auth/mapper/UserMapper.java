package com.migracion.marketplace.auth.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;

import com.migracion.marketplace.auth.dto.CustomerProfileResponse;
import com.migracion.marketplace.auth.dto.CustomerProfileUpdateRequest;
import com.migracion.marketplace.auth.entity.User;

@Mapper(componentModel = "spring", nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
public interface UserMapper {

    CustomerProfileResponse toCustomerProfileResponse(User user);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "email", ignore = true)
    @Mapping(target = "passwordHash", ignore = true)
    @Mapping(target = "role", ignore = true)
    @Mapping(target = "active", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    void updateFromRequest(CustomerProfileUpdateRequest request, @MappingTarget User user);
}
