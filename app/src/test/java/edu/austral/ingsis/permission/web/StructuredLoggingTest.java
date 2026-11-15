package edu.austral.ingsis.permission.web;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.doThrow;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import edu.austral.ingsis.permission.exception.ForbiddenOperationException;
import edu.austral.ingsis.permission.observability.RequestIdFilter;
import edu.austral.ingsis.permission.security.SecurityConfig;
import edu.austral.ingsis.permission.service.PermissionService;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.logging.LoggingInitializationContext;
import org.springframework.boot.logging.LoggingSystem;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.mock.env.MockEnvironment;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Con el formato JSON que se prende en el stack, el contexto del pedido sale como campos que se
 * pueden filtrar, no mezclado en el texto del mensaje.
 */
@WebMvcTest(
        controllers = PermissionController.class,
        properties = "logging.structured.format.console=ecs")
@Import({SecurityConfig.class, JwtCurrentUser.class, RequestIdFilter.class})
@ExtendWith(OutputCaptureExtension.class)
class StructuredLoggingTest {

    private static final String REQUEST_ID = "7f3a91c2-5d1e-4b7a-9c20-0a1b2c3d4e5f";

    @Autowired private MockMvc mockMvc;

    @MockitoBean private PermissionService service;

    @MockitoBean private JwtDecoder jwtDecoder;

    /**
     * Logback se configura una sola vez por JVM: si otro test arranco primero, el formato JSON de
     * esta clase se ignoraria y el resultado dependeria del orden en que corren los tests.
     */
    @BeforeAll
    static void startWithAnUnconfiguredLogging() {
        loggingSystem().cleanUp();
    }

    /** Deja el logging como lo encontraron los demas tests (formato por defecto de Spring Boot). */
    @AfterAll
    static void restoreDefaultLogging() {
        LoggingSystem system = loggingSystem();
        system.cleanUp();
        system.initialize(new LoggingInitializationContext(new MockEnvironment()), null, null);
    }

    private static LoggingSystem loggingSystem() {
        return LoggingSystem.get(StructuredLoggingTest.class.getClassLoader());
    }

    @Test
    void aRefusedOperationIsLoggedAsJsonWithTheRequestContext(CapturedOutput output)
            throws Exception {
        doThrow(new ForbiddenOperationException("Solo el dueño puede compartir"))
                .when(service)
                .share("s1", "auth0|beto", "auth0|carla");

        mockMvc.perform(
                        post("/permissions/s1/share")
                                .header(RequestIdFilter.HEADER, REQUEST_ID)
                                .with(jwt().jwt(token -> token.subject("auth0|beto")))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"userId\":\"auth0|carla\"}"))
                .andExpect(status().isForbidden());

        String line =
                output.getOut()
                        .lines()
                        .filter(l -> l.contains("Operation refused"))
                        .findFirst()
                        .orElseThrow(() -> new AssertionError("no hay log: " + output.getOut()));
        assertTrue(line.startsWith("{"), "no es JSON: " + line);
        assertTrue(line.contains("\"requestId\":\"" + REQUEST_ID + "\""), line);
        assertTrue(line.contains("\"reason\":\"Solo el dueño puede compartir\""), line);
        assertTrue(line.contains("\"userId\":\"auth0|beto\""), line);
        assertTrue(line.contains("\"service\":{\"name\":\"permission-service\""), line);
    }
}
