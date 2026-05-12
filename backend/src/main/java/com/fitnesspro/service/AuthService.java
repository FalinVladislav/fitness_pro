package com.fitnesspro.service;

import com.fitnesspro.dto.Dto.*;
import com.fitnesspro.entity.Client;
import com.fitnesspro.entity.Enums.Role;
import com.fitnesspro.entity.User;
import com.fitnesspro.exception.ApiException;
import com.fitnesspro.repository.ClientRepository;
import com.fitnesspro.repository.UserRepository;
import com.fitnesspro.security.JwtService;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {
    private final UserRepository users;
    private final ClientRepository clients;
    private final PasswordEncoder encoder;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final Mapper mapper;

    public AuthService(UserRepository users, ClientRepository clients, PasswordEncoder encoder,
                       AuthenticationManager authenticationManager, JwtService jwtService, Mapper mapper) {
        this.users = users;
        this.clients = clients;
        this.encoder = encoder;
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
        this.mapper = mapper;
    }

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (users.existsByEmail(request.email())) {
            throw ApiException.badRequest("Пользователь с таким email уже существует");
        }
        User user = new User();
        user.setFullName(request.fullName());
        user.setEmail(request.email());
        user.setPhone(request.phone());
        user.setPasswordHash(encoder.encode(request.password()));
        user.setRole(Role.CLIENT);
        users.save(user);

        Client client = new Client();
        client.setUser(user);
        client.setBirthDate(request.birthDate());
        clients.save(client);
        return new AuthResponse(jwtService.generate(user), mapper.user(user));
    }

    public AuthResponse login(LoginRequest request) {
        authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(request.email(), request.password()));
        User user = userByEmail(request.email());
        return new AuthResponse(jwtService.generate(user), mapper.user(user));
    }

    public User currentUser() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        return userByEmail(email);
    }

    public UserDto me() {
        return mapper.user(currentUser());
    }

    private User userByEmail(String email) {
        return users.findByEmail(email).orElseThrow(() -> ApiException.notFound("Пользователь не найден"));
    }
}
