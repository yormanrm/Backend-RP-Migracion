package com.migracion.marketplace.auth.service;

import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.migracion.marketplace.auth.dto.LoginRequest;
import com.migracion.marketplace.auth.dto.LoginResponse;
import com.migracion.marketplace.auth.dto.RegisterAssociateRequest;
import com.migracion.marketplace.auth.dto.RegisterCustomerRequest;
import com.migracion.marketplace.auth.entity.AssociateProfile;
import com.migracion.marketplace.auth.entity.Role;
import com.migracion.marketplace.auth.entity.User;
import com.migracion.marketplace.auth.repository.AssociateProfileRepository;
import com.migracion.marketplace.auth.repository.UserRepository;
import com.migracion.marketplace.common.exception.DuplicateResourceException;
import com.migracion.marketplace.common.security.JwtService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final AssociateProfileRepository associateProfileRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;

    public LoginResponse registerCustomer(RegisterCustomerRequest request) {
        if (userRepository.existsByEmail(request.email())) {
            throw new DuplicateResourceException("Ya existe una cuenta con el email: " + request.email());
        }
        User user = User.builder()
                .email(request.email())
                .passwordHash(passwordEncoder.encode(request.password()))
                .role(Role.CUSTOMER)
                .firstName(request.firstName())
                .lastName(request.lastName())
                .phone(request.phone())
                .build();
        userRepository.save(user);
        return issueToken(user);
    }

    public LoginResponse registerAssociate(RegisterAssociateRequest request) {
        if (userRepository.existsByEmail(request.email())) {
            throw new DuplicateResourceException("Ya existe una cuenta con el email: " + request.email());
        }
        if (associateProfileRepository.existsByStoreSlug(request.storeSlug())) {
            throw new DuplicateResourceException("Ya existe una tienda con el slug: " + request.storeSlug());
        }
        User user = User.builder()
                .email(request.email())
                .passwordHash(passwordEncoder.encode(request.password()))
                .role(Role.ASSOCIATE)
                .firstName(request.firstName())
                .lastName(request.lastName())
                .phone(request.phone())
                .build();
        userRepository.save(user);

        AssociateProfile profile = AssociateProfile.builder()
                .user(user)
                .storeName(request.storeName())
                .storeSlug(request.storeSlug())
                .taxId(request.taxId())
                .build();
        associateProfileRepository.save(profile);

        return issueToken(user);
    }

    public LoginResponse login(LoginRequest request) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.email(), request.password()));
        User user = userRepository.findByEmail(request.email())
                .orElseThrow(() -> new IllegalStateException("Usuario autenticado no encontrado."));
        return issueToken(user);
    }

    private LoginResponse issueToken(User user) {
        String token = jwtService.generateToken(user);
        return new LoginResponse(token, "Bearer", jwtService.getExpirationSeconds());
    }
}
