package com.edu.silva.budgets.infra.security;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class SecurityConfigurationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void pingIsPublic() throws Exception {
        mockMvc.perform(get("/ping"))
                .andExpect(status().isOk());
    }

    @Test
    void privateEndpointWithoutUserHeaderReturnsUnauthorized() throws Exception {
        mockMvc.perform(get("/budgets"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Não autenticado"));
    }

    @Test
    void invalidUserHeaderReturnsBadRequest() throws Exception {
        mockMvc.perform(get("/budgets").header("X-User-ID", "not-a-uuid"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void privateEndpointWithGatewayHeaderIsAuthorized() throws Exception {
        mockMvc.perform(get("/budgets")
                        .header("X-User-ID", UUID.randomUUID().toString())
                        .header("X-User-Roles", "BUDGETS"))
                .andExpect(status().isOk());
    }
}
