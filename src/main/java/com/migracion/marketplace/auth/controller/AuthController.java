package com.migracion.marketplace.auth.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.migracion.marketplace.auth.dto.LoginRequest;
import com.migracion.marketplace.auth.dto.LoginResponse;
import com.migracion.marketplace.auth.dto.RegisterAssociateRequest;
import com.migracion.marketplace.auth.dto.RegisterCustomerRequest;
import com.migracion.marketplace.auth.service.AuthService;
import com.migracion.marketplace.common.dto.ApiResponse;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register/customer")
    public ResponseEntity<ApiResponse<LoginResponse>> registerCustomer(
            @Valid @RequestBody RegisterCustomerRequest request) {
        LoginResponse response = authService.registerCustomer(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(201, "Cliente registrado exitosamente.", response));
    }

    @PostMapping("/register/associate")
    public ResponseEntity<ApiResponse<LoginResponse>> registerAssociate(
            @Valid @RequestBody RegisterAssociateRequest request) {
        LoginResponse response = authService.registerAssociate(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(201, "Asociado registrado exitosamente.", response));
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<LoginResponse>> login(@Valid @RequestBody LoginRequest request) {
        LoginResponse response = authService.login(request);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}
