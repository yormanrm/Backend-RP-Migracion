package com.migracion.marketplace.auth.service;

import java.util.UUID;

import org.springframework.stereotype.Service;

import com.migracion.marketplace.auth.dto.AssociateProfileResponse;
import com.migracion.marketplace.auth.dto.AssociateProfileUpdateRequest;
import com.migracion.marketplace.auth.dto.CustomerProfileResponse;
import com.migracion.marketplace.auth.dto.CustomerProfileUpdateRequest;
import com.migracion.marketplace.auth.entity.AssociateProfile;
import com.migracion.marketplace.auth.entity.Role;
import com.migracion.marketplace.auth.entity.User;
import com.migracion.marketplace.auth.mapper.AssociateProfileMapper;
import com.migracion.marketplace.auth.mapper.UserMapper;
import com.migracion.marketplace.auth.repository.AssociateProfileRepository;
import com.migracion.marketplace.auth.repository.UserRepository;
import com.migracion.marketplace.common.exception.ResourceNotFoundException;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ProfileService {

    private final UserRepository userRepository;
    private final AssociateProfileRepository associateProfileRepository;
    private final UserMapper userMapper;
    private final AssociateProfileMapper associateProfileMapper;

    public Object getMyProfile(UUID userId) {
        User user = findUser(userId);
        if (user.getRole() == Role.ASSOCIATE) {
            return associateProfileMapper.toResponse(user, findAssociateProfile(userId));
        }
        return userMapper.toCustomerProfileResponse(user);
    }

    public CustomerProfileResponse updateCustomerProfile(UUID userId, CustomerProfileUpdateRequest request) {
        User user = findUser(userId);
        userMapper.updateFromRequest(request, user);
        userRepository.save(user);
        return userMapper.toCustomerProfileResponse(user);
    }

    public AssociateProfileResponse updateAssociateProfile(UUID userId, AssociateProfileUpdateRequest request) {
        User user = findUser(userId);
        user.setFirstName(request.firstName());
        user.setLastName(request.lastName());
        user.setPhone(request.phone());
        userRepository.save(user);

        AssociateProfile profile = findAssociateProfile(userId);
        associateProfileMapper.updateFromRequest(request, profile);
        associateProfileRepository.save(profile);

        return associateProfileMapper.toResponse(user, profile);
    }

    private User findUser(UUID userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado."));
    }

    private AssociateProfile findAssociateProfile(UUID userId) {
        return associateProfileRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Perfil de asociado no encontrado."));
    }
}
