package edu.austral.ingsis.permission.web;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import edu.austral.ingsis.permission.observability.RequestIdFilter;
import edu.austral.ingsis.permission.observability.UserMdcFilter;
import edu.austral.ingsis.permission.security.SecurityConfig;
import edu.austral.ingsis.permission.security.TrustedServiceVerifier;
import edu.austral.ingsis.permission.service.PermissionService;
import edu.austral.ingsis.permission.service.Relation;

import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

/** Los filtros de observabilidad funcionando junto con Spring Security, en el orden correcto. */
@WebMvcTest(PermissionController.class)
@Import({
    SecurityConfig.class,
    JwtCurrentUser.class,
    RequestIdFilter.class,
    TrustedServiceVerifier.class
})
class RequestContextWebTest {

    private static final String REQUEST_ID = "7f3a91c2-5d1e-4b7a-9c20-0a1b2c3d4e5f";

    @Autowired private MockMvc mockMvc;

    @MockitoBean private PermissionService service;

    @MockitoBean private JwtDecoder jwtDecoder;

    @Test
    void answersWithTheRequestIdThatCameWithTheRequest() throws Exception {
        mockMvc.perform(
                        get("/permissions/snippets")
                                .header(RequestIdFilter.HEADER, REQUEST_ID)
                                .with(jwt().jwt(token -> token.subject("auth0|ana"))))
                .andExpect(status().isOk())
                .andExpect(header().string(RequestIdFilter.HEADER, REQUEST_ID));
    }

    @Test
    void aRequestRejectedBySecurityAlsoGetsARequestId() throws Exception {
        // Sin token: lo corta Spring Security con un 401. El filtro del requestId tiene que haber
        // corrido antes, si no justo los pedidos que fallan quedarian sin identificador.
        mockMvc.perform(get("/permissions/snippets").header(RequestIdFilter.HEADER, REQUEST_ID))
                .andExpect(status().isUnauthorized())
                .andExpect(header().string(RequestIdFilter.HEADER, REQUEST_ID));
    }

    @Test
    void theServiceSeesWhoAndWhichRequestInTheLogContext() throws Exception {
        AtomicReference<String> userId = new AtomicReference<>();
        AtomicReference<String> requestId = new AtomicReference<>();
        when(service.listSnippets("auth0|ana", Relation.ALL))
                .thenAnswer(
                        invocation -> {
                            userId.set(MDC.get(UserMdcFilter.MDC_KEY));
                            requestId.set(MDC.get(RequestIdFilter.MDC_KEY));
                            return List.of();
                        });

        mockMvc.perform(
                        get("/permissions/snippets")
                                .header(RequestIdFilter.HEADER, REQUEST_ID)
                                .with(jwt().jwt(token -> token.subject("auth0|ana"))))
                .andExpect(status().isOk());

        assertEquals("auth0|ana", userId.get());
        assertEquals(REQUEST_ID, requestId.get());
    }
}
