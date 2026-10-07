package edu.austral.ingsis.permission.web;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import edu.austral.ingsis.permission.exception.AlreadyExistsException;
import edu.austral.ingsis.permission.exception.ForbiddenOperationException;
import edu.austral.ingsis.permission.model.Role;
import edu.austral.ingsis.permission.service.PermissionService;
import edu.austral.ingsis.permission.service.Relation;
import edu.austral.ingsis.permission.service.SnippetAccess;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(PermissionController.class)
@Import(HeaderCurrentUser.class)
class PermissionControllerTest {

    private static final String USER_HEADER = "X-User-Id";
    private static final String USER = "auth0|ana";

    @Autowired private MockMvc mockMvc;

    @MockitoBean private PermissionService service;

    @Test
    void registerOwnerReturnsCreated() throws Exception {
        mockMvc.perform(
                        post("/permissions")
                                .header(USER_HEADER, USER)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"snippetId\":\"s1\"}"))
                .andExpect(status().isCreated());

        verify(service).registerOwner("s1", USER);
    }

    @Test
    void registerOwnerWithBlankSnippetIdIsBadRequest() throws Exception {
        mockMvc.perform(
                        post("/permissions")
                                .header(USER_HEADER, USER)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"snippetId\":\" \"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void registerOwnerConflictReturns409() throws Exception {
        doThrow(new AlreadyExistsException("ya existe")).when(service).registerOwner("s1", USER);

        mockMvc.perform(
                        post("/permissions")
                                .header(USER_HEADER, USER)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"snippetId\":\"s1\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("ya existe"));
    }

    @Test
    void missingUserHeaderReturns401() throws Exception {
        mockMvc.perform(get("/permissions/s1")).andExpect(status().isUnauthorized());
    }

    @Test
    void shareReturnsNoContent() throws Exception {
        mockMvc.perform(
                        post("/permissions/s1/share")
                                .header(USER_HEADER, USER)
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
                                .header(USER_HEADER, USER)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"userId\":\"auth0|beto\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void listSnippetsUsesRelationParameter() throws Exception {
        when(service.listSnippets(USER, Relation.SHARED))
                .thenReturn(List.of(new SnippetAccess("s1", Role.READER)));

        mockMvc.perform(
                        get("/permissions/snippets")
                                .param("relation", "shared")
                                .header(USER_HEADER, USER))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].snippetId").value("s1"))
                .andExpect(jsonPath("$[0].role").value("READER"));
    }

    @Test
    void listSnippetsDefaultsToAll() throws Exception {
        when(service.listSnippets(USER, Relation.ALL)).thenReturn(List.of());

        mockMvc.perform(get("/permissions/snippets").header(USER_HEADER, USER))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isEmpty());
    }

    @Test
    void listSnippetsWithInvalidRelationReturns400() throws Exception {
        mockMvc.perform(
                        get("/permissions/snippets")
                                .param("relation", "nope")
                                .header(USER_HEADER, USER))
                .andExpect(status().isBadRequest());
    }

    @Test
    void roleOfReturnsRole() throws Exception {
        when(service.roleOf("s1", USER)).thenReturn(Role.OWNER);

        mockMvc.perform(get("/permissions/s1").header(USER_HEADER, USER))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role").value("OWNER"));
    }

    @Test
    void deleteReturnsNoContent() throws Exception {
        mockMvc.perform(delete("/permissions/s1").header(USER_HEADER, USER))
                .andExpect(status().isNoContent());

        verify(service).deleteSnippet("s1", USER);
    }

    @Test
    void deleteByNonOwnerReturns403() throws Exception {
        doThrow(new ForbiddenOperationException("no sos dueño"))
                .when(service)
                .deleteSnippet(any(), any());

        mockMvc.perform(delete("/permissions/s1").header(USER_HEADER, USER))
                .andExpect(status().isForbidden());
    }
}
