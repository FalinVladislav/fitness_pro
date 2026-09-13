package com.fitnesspro.controller;

import com.fitnesspro.config.SecurityConfig;
import com.fitnesspro.exception.GlobalExceptionHandler;
import com.fitnesspro.repository.UserRepository;
import com.fitnesspro.security.JwtAuthenticationFilter;
import com.fitnesspro.security.JwtService;
import com.fitnesspro.service.ClientService;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Import;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = ClientController.class,
        excludeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = GlobalExceptionHandler.class))
@Import({SecurityConfig.class, ClientSecurityTest.FilterConfiguration.class})
class ClientSecurityTest {
    @Autowired private MockMvc mockMvc;

    @MockBean private ClientService clients;
    @MockBean private UserRepository users;
    @MockBean private JwtService jwtService;

    @Test
    void clientsEndpointRejectsRequestWithoutToken() throws Exception {
        mockMvc.perform(get("/api/clients"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "CLIENT")
    void clientRoleCannotReadAdminClientList() throws Exception {
        mockMvc.perform(get("/api/clients"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void adminCanReadClientList() throws Exception {
        when(clients.all()).thenReturn(List.of());

        mockMvc.perform(get("/api/clients"))
                .andExpect(status().isOk());
    }

    @TestConfiguration
    static class FilterConfiguration {
        @Bean
        JwtAuthenticationFilter jwtAuthenticationFilter(JwtService jwtService, UserDetailsService userDetailsService) {
            return new JwtAuthenticationFilter(jwtService, userDetailsService);
        }
    }
}
