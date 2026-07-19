package com.migracion.marketplace.common.exception;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.authentication.BadCredentialsException;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();
    private final MockHttpServletRequest request = new MockHttpServletRequest();

    @Test
    void mapsAuthenticationExceptionTo401() {
        ProblemDetail detail = handler.handleAuthentication(new BadCredentialsException("credenciales invalidas"), request);
        assertEquals(HttpStatus.UNAUTHORIZED.value(), detail.getStatus());
    }

    @Test
    void mapsResourceNotFoundTo404() {
        ProblemDetail detail = handler.handleNotFound(new ResourceNotFoundException("no existe"), request);
        assertEquals(HttpStatus.NOT_FOUND.value(), detail.getStatus());
    }

    @Test
    void mapsDuplicateResourceTo409() {
        ProblemDetail detail = handler.handleDuplicate(new DuplicateResourceException("duplicado"), request);
        assertEquals(HttpStatus.CONFLICT.value(), detail.getStatus());
    }

    @Test
    void mapsInsufficientStockTo422() {
        ProblemDetail detail = handler.handleInsufficientStock(new InsufficientStockException("sin stock"), request);
        assertEquals(HttpStatus.UNPROCESSABLE_ENTITY.value(), detail.getStatus());
    }

    @Test
    void mapsForbiddenOperationTo403() {
        ProblemDetail detail = handler.handleForbidden(new ForbiddenOperationException("prohibido"), request);
        assertEquals(HttpStatus.FORBIDDEN.value(), detail.getStatus());
    }

    @Test
    void mapsGenericExceptionTo500() {
        ProblemDetail detail = handler.handleGeneric(new RuntimeException("boom"), request);
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR.value(), detail.getStatus());
    }
}
