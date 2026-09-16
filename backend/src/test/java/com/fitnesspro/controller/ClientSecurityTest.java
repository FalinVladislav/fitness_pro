package com.fitnesspro.controller;

import com.fitnesspro.config.SecurityConfig;
import com.fitnesspro.exception.ApiException;
import com.fitnesspro.repository.UserRepository;
import com.fitnesspro.security.JwtAuthenticationFilter;
import com.fitnesspro.security.JwtService;
import com.fitnesspro.service.ClientService;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = ClientController.class)
@Import({SecurityConfig.class, ClientSecurityTest.FilterConfiguration.class})
class ClientSecurityTest {
    @Autowired private MockMvc mockMvc;

    @MockBean private ClientService clients;
    @MockBean private UserRepository users;
    @MockBean private JwtService jwtService;

    @Test
    void clientsEndpointRejectsRequestWithoutToken() throws Exception {
        mockMvc.perform(get("/api/clients"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.error").value("Unauthorized"))
                .andExpect(jsonPath("$.message").value("Требуется аутентификация"));
    }

    @Test
    void clientsEndpointRejectsInvalidTokenWithUnifiedBody() throws Exception {
        mockMvc.perform(get("/api/clients").header("Authorization", "Bearer invalid-token"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.error").value("Unauthorized"))
                .andExpect(jsonPath("$.message").value("Требуется аутентификация"));
    }

    @Test
    @WithMockUser(roles = "CLIENT")
    void clientRoleCannotReadAdminClientList() throws Exception {
        mockMvc.perform(get("/api/clients"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.error").value("Forbidden"))
                .andExpect(jsonPath("$.message").value("Недостаточно прав для выполнения операции"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void adminCanReadClientList() throws Exception {
        when(clients.all()).thenReturn(List.of());

        mockMvc.perform(get("/api/clients"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void invalidRequestReturnsUnifiedBadRequestBody() throws Exception {
        mockMvc.perform(post("/api/clients")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"fullName\":\"\",\"email\":\"not-an-email\",\"phone\":\"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Bad Request"))
                .andExpect(jsonPath("$.message").isNotEmpty())
                .andExpect(jsonPath("$.timestamp").exists());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void missingClientReturnsUnifiedNotFoundBody() throws Exception {
        when(clients.get(99L)).thenThrow(ApiException.notFound("Клиент не найден"));

        mockMvc.perform(get("/api/clients/99"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("Not Found"))
                .andExpect(jsonPath("$.message").value("Клиент не найден"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void unexpectedErrorDoesNotExposeInternalDetails() throws Exception {
        when(clients.all()).thenThrow(new IllegalStateException("database credentials are invalid"));

        mockMvc.perform(get("/api/clients"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.status").value(500))
                .andExpect(jsonPath("$.error").value("Internal Server Error"))
                .andExpect(jsonPath("$.message").value("Внутренняя ошибка сервера"));
    }

    @TestConfiguration
    static class FilterConfiguration {
        @Bean
        JwtAuthenticationFilter jwtAuthenticationFilter(JwtService jwtService, UserDetailsService userDetailsService) {
            return new JwtAuthenticationFilter(jwtService, userDetailsService);
        }
    }
}
