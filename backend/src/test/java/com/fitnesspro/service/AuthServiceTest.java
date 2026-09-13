package com.fitnesspro.service;

import com.fitnesspro.dto.Dto.AuthResponse;
import com.fitnesspro.dto.Dto.LoginRequest;
import com.fitnesspro.dto.Dto.RegisterRequest;
import com.fitnesspro.dto.Dto.UserDto;
import com.fitnesspro.entity.Client;
import com.fitnesspro.entity.Enums.Role;
import com.fitnesspro.entity.User;
import com.fitnesspro.repository.ClientRepository;
import com.fitnesspro.repository.UserRepository;
import com.fitnesspro.security.JwtService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {
    @Mock private UserRepository users;
    @Mock private ClientRepository clients;
    @Mock private PasswordEncoder encoder;
    @Mock private AuthenticationManager authenticationManager;
    @Mock private JwtService jwtService;
    @Mock private Mapper mapper;

    @Test
    void registerCreatesClientAccountAndReturnsToken() {
        AuthService service = service();
        RegisterRequest request = new RegisterRequest("Ирина", "irina@example.com", "+79990000000", "password", LocalDate.of(2000, 1, 1));
        when(users.existsByEmail(request.email())).thenReturn(false);
        when(encoder.encode(request.password())).thenReturn("hash");
        when(users.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(jwtService.generate(any(User.class))).thenReturn("jwt");
        when(mapper.user(any(User.class))).thenAnswer(invocation -> userDto(invocation.getArgument(0, User.class)));

        AuthResponse response = service.register(request);

        ArgumentCaptor<Client> clientCaptor = ArgumentCaptor.forClass(Client.class);
        verify(clients).save(clientCaptor.capture());
        User registered = clientCaptor.getValue().getUser();
        assertThat(registered.getEmail()).isEqualTo(request.email());
        assertThat(registered.getRole()).isEqualTo(Role.CLIENT);
        assertThat(registered.getPasswordHash()).isEqualTo("hash");
        assertThat(response.token()).isEqualTo("jwt");
        verify(encoder).encode("password");
    }

    @Test
    void loginReturnsTokenAfterSuccessfulAuthentication() {
        AuthService service = service();
        User user = user(Role.CLIENT);
        LoginRequest request = new LoginRequest(user.getEmail(), "password");
        when(authenticationManager.authenticate(any(Authentication.class))).thenReturn(mock(Authentication.class));
        when(users.findByEmail(user.getEmail())).thenReturn(Optional.of(user));
        when(jwtService.generate(user)).thenReturn("jwt");
        when(mapper.user(user)).thenReturn(userDto(user));

        AuthResponse response = service.login(request);

        assertThat(response.token()).isEqualTo("jwt");
        verify(authenticationManager).authenticate(any(Authentication.class));
    }

    @Test
    void loginPropagatesInvalidPasswordError() {
        AuthService service = service();
        LoginRequest request = new LoginRequest("irina@example.com", "wrong-password");
        when(authenticationManager.authenticate(any(Authentication.class))).thenThrow(new BadCredentialsException("Bad credentials"));

        assertThatThrownBy(() -> service.login(request))
                .isInstanceOf(BadCredentialsException.class);

        verify(users, never()).findByEmail(any());
    }

    private AuthService service() {
        return new AuthService(users, clients, encoder, authenticationManager, jwtService, mapper);
    }

    private User user(Role role) {
        User user = new User();
        user.setId(1L);
        user.setEmail("irina@example.com");
        user.setFullName("Ирина");
        user.setPhone("+79990000000");
        user.setRole(role);
        return user;
    }

    private UserDto userDto(User user) {
        return new UserDto(user.getId(), user.getFullName(), user.getEmail(), user.getPhone(), user.getRole(), true);
    }
}
