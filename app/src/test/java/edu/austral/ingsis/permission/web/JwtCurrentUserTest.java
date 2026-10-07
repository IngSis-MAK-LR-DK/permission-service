package edu.austral.ingsis.permission.web;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import edu.austral.ingsis.permission.exception.UnauthenticatedException;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

class JwtCurrentUserTest {

    private final JwtCurrentUser currentUser = new JwtCurrentUser();

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void returnsTheSubjectOfTheToken() {
        authenticate(Jwt.withTokenValue("t").header("alg", "none").subject("auth0|ana").build());

        assertEquals("auth0|ana", currentUser.userId());
    }

    @Test
    void failsWithoutAuthentication() {
        assertThrows(UnauthenticatedException.class, currentUser::userId);
    }

    @Test
    void failsWhenTokenHasNoSubject() {
        authenticate(Jwt.withTokenValue("t").header("alg", "none").claim("x", "y").build());

        assertThrows(UnauthenticatedException.class, currentUser::userId);
    }

    private static void authenticate(Jwt jwt) {
        SecurityContextHolder.getContext().setAuthentication(new JwtAuthenticationToken(jwt));
    }
}
