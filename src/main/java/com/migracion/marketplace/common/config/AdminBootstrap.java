package com.migracion.marketplace.common.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import com.migracion.marketplace.auth.entity.Role;
import com.migracion.marketplace.auth.entity.User;
import com.migracion.marketplace.auth.repository.UserRepository;

import lombok.RequiredArgsConstructor;

/**
 * Bootstrap del primer ADMIN: si no existe ningún usuario con rol ADMIN al arrancar,
 * crea uno con las credenciales de configuración (app.admin.email / app.admin.password).
 * Los siguientes administradores se crean vía POST /auth/register/admin (solo ADMIN).
 */
@Component
@RequiredArgsConstructor
public class AdminBootstrap implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(AdminBootstrap.class);

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.admin.email:}")
    private String adminEmail;

    @Value("${app.admin.password:}")
    private String adminPassword;

    @Override
    public void run(ApplicationArguments args) {
        if (userRepository.existsByRole(Role.ADMIN)) {
            return;
        }
        if (adminEmail.isBlank() || adminPassword.isBlank()) {
            log.warn("No existe ningún ADMIN y no hay credenciales configuradas (app.admin.email / app.admin.password). "
                    + "No se creó el administrador inicial.");
            return;
        }
        userRepository.save(User.builder()
                .email(adminEmail)
                .passwordHash(passwordEncoder.encode(adminPassword))
                .role(Role.ADMIN)
                .firstName("Admin")
                .lastName("Marketplace")
                .build());
        log.info("Administrador inicial creado con email {}", adminEmail);
    }
}
