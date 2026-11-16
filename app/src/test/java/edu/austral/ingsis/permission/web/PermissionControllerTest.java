package edu.austral.ingsis.permission.web;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import edu.austral.ingsis.permission.exception.AlreadyExistsException;
import edu.austral.ingsis.permission.exception.ForbiddenOperationException;
import edu.austral.ingsis.permission.model.Role;
import edu.austral.ingsis.permission.security.SecurityConfig;
import edu.austral.ingsis.permission.security.TrustedServiceVerifier;
import edu.austral.ingsis.permission.service.PermissionService;
import edu.austral.ingsis.permission.service.Relation;
import edu.austral.ingsis.permission.service.SnippetAccess;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.JwtRequestPostProcessor;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(PermissionController.class)
@Import({SecurityConfig.class, JwtCurrentUser.class, TrustedServiceVerifier.class})
class PermissionControllerTest {

    private static final String USER = "auth0|ana";
    private static final String SERVICE_CLIENT = "test-service-client";

    @Autowired private MockMvc mockMvc;

    @MockitoBean private PermissionService service;

    @MockitoBean private JwtDecoder jwtDecoder;

    @Test
    void registerOwnerByTheHandlerReturnsCreated() throws Exception {
        mockMvc.perform(
                        post("/permissions")
                                .with(asService())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"snippetId\":\"s1\",\"ownerId\":\"" + USER + "\"}"))
                .andExpect(status().isCreated());

        // El dueño es el que viaja en el body, no el sub del token (que es el del servicio).
        verify(service).registerOwner("s1", USER);
    }

    @Test
    void registerOwnerByAUserIsForbidden() throws Exception {
        // Un usuario autenticado no puede declararse dueño de un snippet, ni siquiera de si mismo.
        mockMvc.perform(
                        post("/permissions")
                                .with(asUser(USER))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"snippetId\":\"s1\",\"ownerId\":\"" + USER + "\"}"))
                .andExpect(status().isForbidden());

        verifyNoInteractions(service);
    }

    @Test
    void registerOwnerByAnotherAppIsForbidden() throws Exception {
        mockMvc.perform(
                        post("/permissions")
                                .with(jwt().jwt(token -> token.claim("azp", "otra-app")))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"snippetId\":\"s1\",\"ownerId\":\"" + USER + "\"}"))
                .andExpect(status().isForbidden());

        verifyNoInteractions(service);
    }

    @Test
    void registerOwnerWithoutTokenIsUnauthorized() throws Exception {
        mockMvc.perform(
                        post("/permissions")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"snippetId\":\"s1\",\"ownerId\":\"" + USER + "\"}"))
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(service);
    }

    @Test
    void registerOwnerWithBlankSnippetIdIsBadRequest() throws Exception {
        mockMvc.perform(
                        post("/permissions")
                                .with(asService())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"snippetId\":\" \",\"ownerId\":\"" + USER + "\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void registerOwnerWithoutOwnerIsBadRequest() throws Exception {
        mockMvc.perform(
                        post("/permissions")
                                .with(asService())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"snippetId\":\"s1\"}"))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(service);
    }

    @Test
    void registerOwnerConflictReturns409() throws Exception {
        doThrow(new AlreadyExistsException("ya existe")).when(service).registerOwner("s1", USER);

        mockMvc.perform(
                        post("/permissions")
                                .with(asService())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"snippetId\":\"s1\",\"ownerId\":\"" + USER + "\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("ya existe"));
    }

    @Test
    void missingTokenReturns401() throws Exception {
        mockMvc.perform(get("/permissions/s1")).andExpect(status().isUnauthorized());
    }

    @Test
    void shareReturnsNoContent() throws Exception {
        mockMvc.perform(
                        post("/permissions/s1/share")
                                .with(asUser(USER))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"userId\":\"auth0|beto\"}"))
                .andExpect(status().isNoContent());

        verify(service).share("s1", USER, "auth0|beto");
    }

    @Test
    void shareByNonOwnerReturns403() throws Exception {
        doThrow(new ForbiddenOperationException("no sos dueño"))
                .when(service)
                .share("s1", USER, "auth0|beto");

        mockMvc.perform(
                        post("/permissions/s1/share")
                                .with(asUser(USER))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"userId\":\"auth0|beto\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void listSnippetsUsesRelationParameter() throws Exception {
        when(service.listSnippets(USER, Relation.SHARED))
                .thenReturn(List.of(new SnippetAccess("s1", Role.READER)));

        mockMvc.perform(get("/permissions/snippets").param("relation", "shared").with(asUser(USER)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].snippetId").value("s1"))
                .andExpect(jsonPath("$[0].role").value("READER"));
    }

    @Test
    void listSnippetsDefaultsToAll() throws Exception {
        when(service.listSnippets(USER, Relation.ALL)).thenReturn(List.of());

        mockMvc.perform(get("/permissions/snippets").with(asUser(USER)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isEmpty());
    }

    @Test
    void listSnippetsWithInvalidRelationReturns400() throws Exception {
        mockMvc.perform(get("/permissions/snippets").param("relation", "nope").with(asUser(USER)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void roleOfReturnsRole() throws Exception {
        when(service.roleOf("s1", USER)).thenReturn(Role.OWNER);

        mockMvc.perform(get("/permissions/s1").with(asUser(USER)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role").value("OWNER"));
    }

    @Test
    void deleteReturnsNoContent() throws Exception {
        mockMvc.perform(delete("/permissions/s1").with(asUser(USER)))
                .andExpect(status().isNoContent());

        verify(service).deleteSnippet("s1", USER);
    }

    @Test
    void deleteByNonOwnerReturns403() throws Exception {
        doThrow(new ForbiddenOperationException("no sos dueño"))
                .when(service)
                .deleteSnippet(any(), any());

        mockMvc.perform(delete("/permissions/s1").with(asUser(USER)))
                .andExpect(status().isForbidden());
    }

    private static JwtRequestPostProcessor asUser(String userId) {
        return jwt().jwt(token -> token.subject(userId));
    }

    /** Un token pedido por la app M2M del handler: el sub es "<client_id>@clients". */
    private static JwtRequestPostProcessor asService() {
        return jwt().jwt(
                        token ->
                                token.subject(SERVICE_CLIENT + "@clients")
                                        .claim("azp", SERVICE_CLIENT));
    }
}
