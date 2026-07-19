package com.migracion.marketplace.auth.controller;

import java.util.UUID;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.migracion.marketplace.auth.dto.AssociateProfileResponse;
import com.migracion.marketplace.auth.dto.AssociateProfileUpdateRequest;
import com.migracion.marketplace.auth.dto.CustomerProfileResponse;
import com.migracion.marketplace.auth.dto.CustomerProfileUpdateRequest;
import com.migracion.marketplace.auth.service.ProfileService;
import com.migracion.marketplace.common.dto.ApiResponse;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/profile")
@RequiredArgsConstructor
public class ProfileController {

    private final ProfileService profileService;

    @GetMapping("/me")
    public ApiResponse<?> getMyProfile(@AuthenticationPrincipal Jwt jwt) {
        return ApiResponse.success(profileService.getMyProfile(userId(jwt)));
    }

    @PutMapping("/me/customer")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ApiResponse<CustomerProfileResponse> updateCustomerProfile(@AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody CustomerProfileUpdateRequest request) {
        return ApiResponse.success(profileService.updateCustomerProfile(userId(jwt), request));
    }

    @PutMapping("/me/associate")
    @PreAuthorize("hasRole('ASSOCIATE')")
    public ApiResponse<AssociateProfileResponse> updateAssociateProfile(@AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody AssociateProfileUpdateRequest request) {
        return ApiResponse.success(profileService.updateAssociateProfile(userId(jwt), request));
    }

    private UUID userId(Jwt jwt) {
        return UUID.fromString(jwt.getSubject());
    }
}
