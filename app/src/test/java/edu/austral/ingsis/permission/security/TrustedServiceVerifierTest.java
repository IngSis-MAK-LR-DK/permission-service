package edu.austral.ingsis.permission.security;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;

import edu.austral.ingsis.permission.exception.ForbiddenOperationException;
import edu.austral.ingsis.permission.exception.UnauthenticatedException;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

class TrustedServiceVerifierTest {

    private static final String SERVICE_CLIENT = "handler-client-id";

    private final TrustedServiceVerifier verifier = new TrustedServiceVerifier(SERVICE_CLIENT);

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void acceptsATokenRequestedByTheHandlersApp() {
        authenticateWith(tokenOf("handler-client-id@clients", SERVICE_CLIENT));

        assertDoesNotThrow(verifier::requireTrustedService);
    }

    @ParameterizedTest
    @ValueSource(strings = {"spa-client-id", "otra-app", "HANDLER-CLIENT-ID", "handler-client-id "})
    void rejectsATokenRequestedByAnotherApp(String clientId) {
        authenticateWith(tokenOf("auth0|ana", clientId));

        assertThrows(ForbiddenOperationException.class, verifier::requireTrustedService);
    }

    @Test
    void rejectsATokenWithoutTheAuthorizedPartyClaim() {
        authenticateWith(tokenOf("auth0|ana", null));

        assertThrows(ForbiddenOperationException.class, verifier::requireTrustedService);
    }

    @Test
    void withoutAuthenticationItIsUnauthenticated() {
        assertThrows(UnauthenticatedException.class, verifier::requireTrustedService);
    }

    @Test
    void anAuthenticationThatIsNotAnAuth0TokenIsUnauthenticated() {
        SecurityContextHolder.getContext()
                .setAuthentication(new UsernamePasswordAuthenticationToken("ana", "n/a"));

        assertThrows(UnauthenticatedException.class, verifier::requireTrustedService);
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" ", "   "})
    void refusesToStartWithoutAConfiguredClientId(String configured) {
        assertThrows(IllegalArgumentException.class, () -> new TrustedServiceVerifier(configured));
    }

    private static void authenticateWith(Jwt jwt) {
        SecurityContextHolder.getContext()
                .setAuthentication(new JwtAuthenticationToken(jwt, List.of()));
    }

    private static Jwt tokenOf(String subject, String authorizedParty) {
        Jwt.Builder builder = Jwt.withTokenValue("token").header("alg", "none").subject(subject);
        if (authorizedParty != null) {
            builder.claim(TrustedServiceVerifier.AUTHORIZED_PARTY, authorizedParty);
        }
        return builder.build();
    }
}
