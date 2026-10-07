package edu.austral.ingsis.permission;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.http.MediaType;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.JwtRequestPostProcessor;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/** Recorre un caso completo con Flyway, JPA y un Postgres real en un contenedor. */
@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
class PermissionFlowIntegrationTest {

    private static final String ANA = "auth0|ana";
    private static final String BETO = "auth0|beto";

    @Container @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16");

    @Autowired private MockMvc mockMvc;

    @Test
    void ownerSharesAndDeletesSnippet() throws Exception {
        mockMvc.perform(
                        post("/permissions")
                                .with(asUser(ANA))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"snippetId\":\"flow-1\"}"))
                .andExpect(status().isCreated());

        mockMvc.perform(
                        post("/permissions")
                                .with(asUser(BETO))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"snippetId\":\"flow-1\"}"))
                .andExpect(status().isConflict());

        mockMvc.perform(
                        post("/permissions/flow-1/share")
                                .with(asUser(BETO))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"userId\":\"auth0|carla\"}"))
                .andExpect(status().isForbidden());

        mockMvc.perform(
                        post("/permissions/flow-1/share")
                                .with(asUser(ANA))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"userId\":\"" + BETO + "\"}"))
                .andExpect(status().isNoContent());

        mockMvc.perform(
                        post("/permissions/flow-1/share")
                                .with(asUser(ANA))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"userId\":\"" + BETO + "\"}"))
                .andExpect(status().isConflict());

        mockMvc.perform(get("/permissions/flow-1").with(asUser(BETO)))
                .andExpect(jsonPath("$.role").value("READER"));

        mockMvc.perform(get("/permissions/snippets").param("relation", "shared").with(asUser(BETO)))
                .andExpect(jsonPath("$[0].snippetId").value("flow-1"));

        mockMvc.perform(get("/permissions/snippets").param("relation", "owner").with(asUser(BETO)))
                .andExpect(jsonPath("$").isEmpty());

        mockMvc.perform(delete("/permissions/flow-1").with(asUser(BETO)))
                .andExpect(status().isForbidden());
        mockMvc.perform(delete("/permissions/flow-1").with(asUser(ANA)))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/permissions/flow-1").with(asUser(BETO)))
                .andExpect(jsonPath("$.role").value("NONE"));
    }

    @Test
    void requestsWithoutTokenAreRejected() throws Exception {
        mockMvc.perform(get("/permissions/any")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/health")).andExpect(status().isOk());
    }

    private static JwtRequestPostProcessor asUser(String userId) {
        return jwt().jwt(token -> token.subject(userId));
    }
}
