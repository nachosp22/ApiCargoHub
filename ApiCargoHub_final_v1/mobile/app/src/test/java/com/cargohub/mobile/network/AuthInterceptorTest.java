package com.cargohub.mobile.network;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class AuthInterceptorTest {

    @Test
    public void shouldInvalidateSession_returnsTrueForUnauthorizedStatusOnProtectedEndpoints() {
        assertTrue(AuthInterceptor.shouldInvalidateSession("/api/portes/99", 401, true, null));
    }

    @Test
    public void shouldInvalidateSession_returnsFalseForForbiddenStatusOnProtectedEndpoints() {
        assertFalse(AuthInterceptor.shouldInvalidateSession("/api/portes/99", 403, true, null));
    }

    @Test
    public void shouldInvalidateSession_returnsFalseForLoginEndpoint() {
        assertFalse(AuthInterceptor.shouldInvalidateSession("/api/auth/login", 401, true, null));
    }

    @Test
    public void shouldInvalidateSession_returnsFalseWhenRequestHasNoBearerHeader() {
        assertFalse(AuthInterceptor.shouldInvalidateSession("/api/portes/99", 401, false, null));
    }

    @Test
    public void shouldInvalidateSession_returnsFalseForAccessDeniedPayloadEvenOn401() {
        assertFalse(AuthInterceptor.shouldInvalidateSession(
                "/api/portes/99",
                401,
                true,
                "{\"message\":\"Access denied\"}"
        ));
    }

    @Test
    public void shouldInvalidateSession_returnsTrueForInvalidTokenPayload() {
        assertTrue(AuthInterceptor.shouldInvalidateSession(
                "/api/portes/99",
                401,
                true,
                "{\"message\":\"JWT token expired\"}"
        ));
    }
}
