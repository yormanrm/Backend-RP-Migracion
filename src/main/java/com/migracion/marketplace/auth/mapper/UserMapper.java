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

    @Mapping(source = "shippingAddress.street", target = "street")
    @Mapping(source = "shippingAddress.city", target = "city")
    @Mapping(source = "shippingAddress.state", target = "state")
    @Mapping(source = "shippingAddress.postalCode", target = "postalCode")
    @Mapping(source = "shippingAddress.country", target = "country")
    CustomerProfileResponse toCustomerProfileResponse(User user);

    @Mapping(target = "shippingAddress.street", source = "street")
    @Mapping(target = "shippingAddress.city", source = "city")
    @Mapping(target = "shippingAddress.state", source = "state")
    @Mapping(target = "shippingAddress.postalCode", source = "postalCode")
    @Mapping(target = "shippingAddress.country", source = "country")
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "email", ignore = true)
    @Mapping(target = "passwordHash", ignore = true)
    @Mapping(target = "role", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    void updateFromRequest(CustomerProfileUpdateRequest request, @MappingTarget User user);
}
