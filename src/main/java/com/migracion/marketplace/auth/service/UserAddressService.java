package com.migracion.marketplace.auth.service;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.migracion.marketplace.auth.dto.AddressDto;
import com.migracion.marketplace.auth.dto.UserAddressResponse;
import com.migracion.marketplace.auth.entity.User;
import com.migracion.marketplace.auth.entity.UserAddress;
import com.migracion.marketplace.auth.repository.UserAddressRepository;
import com.migracion.marketplace.auth.repository.UserRepository;
import com.migracion.marketplace.common.exception.ResourceNotFoundException;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class UserAddressService {

    private final UserAddressRepository userAddressRepository;
    private final UserRepository userRepository;

    public List<UserAddressResponse> list(UUID userId) {
        return userAddressRepository.findByUserIdOrderByCreatedAtAsc(userId).stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public UserAddressResponse create(UUID userId, AddressDto request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado."));
        // La primera dirección del usuario queda como predeterminada automáticamente.
        boolean first = !userAddressRepository.existsByUserId(userId);
        UserAddress address = UserAddress.builder()
                .user(user)
                .street(request.street())
                .city(request.city())
                .state(request.state())
                .postalCode(request.postalCode())
                .country(request.country())
                .isDefault(first)
                .build();
        return toResponse(userAddressRepository.save(address));
    }

    @Transactional
    public UserAddressResponse update(UUID userId, UUID addressId, AddressDto request) {
        UserAddress address = findOwned(userId, addressId);
        address.setStreet(request.street());
        address.setCity(request.city());
        address.setState(request.state());
        address.setPostalCode(request.postalCode());
        address.setCountry(request.country());
        return toResponse(userAddressRepository.save(address));
    }

    @Transactional
    public void delete(UUID userId, UUID addressId) {
        UserAddress address = findOwned(userId, addressId);
        boolean wasDefault = address.isDefault();
        userAddressRepository.delete(address);
        // Si se elimina la predeterminada, la más antigua restante pasa a serlo.
        if (wasDefault) {
            userAddressRepository.findByUserIdOrderByCreatedAtAsc(userId).stream()
                    .findFirst()
                    .ifPresent(remaining -> {
                        remaining.setDefault(true);
                        userAddressRepository.save(remaining);
                    });
        }
    }

    @Transactional
    public UserAddressResponse markDefault(UUID userId, UUID addressId) {
        UserAddress address = findOwned(userId, addressId);
        userAddressRepository.findByUserIdAndIsDefaultTrue(userId)
                .filter(current -> !current.getId().equals(address.getId()))
                .ifPresent(current -> {
                    current.setDefault(false);
                    userAddressRepository.save(current);
                });
        address.setDefault(true);
        return toResponse(userAddressRepository.save(address));
    }

    public UserAddress resolveForCheckout(UUID userId, UUID addressId) {
        if (addressId != null) {
            return findOwned(userId, addressId);
        }
        return userAddressRepository.findByUserIdAndIsDefaultTrue(userId).orElse(null);
    }

    private UserAddress findOwned(UUID userId, UUID addressId) {
        return userAddressRepository.findByIdAndUserId(addressId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Dirección no encontrada."));
    }

    private UserAddressResponse toResponse(UserAddress address) {
        return new UserAddressResponse(address.getId(), address.getStreet(), address.getCity(),
                address.getState(), address.getPostalCode(), address.getCountry(), address.isDefault());
    }
}
