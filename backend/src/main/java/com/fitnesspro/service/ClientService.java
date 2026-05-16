package com.fitnesspro.service;

import com.fitnesspro.dto.Dto.*;
import com.fitnesspro.entity.Client;
import com.fitnesspro.entity.Enums.Role;
import com.fitnesspro.entity.User;
import com.fitnesspro.exception.ApiException;
import com.fitnesspro.repository.ClientRepository;
import com.fitnesspro.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class ClientService {
    private final ClientRepository clients;
    private final UserRepository users;
    private final PasswordEncoder encoder;
    private final Mapper mapper;

    public ClientService(ClientRepository clients, UserRepository users, PasswordEncoder encoder, Mapper mapper) {
        this.clients = clients;
        this.users = users;
        this.encoder = encoder;
        this.mapper = mapper;
    }

    public List<ClientDto> all() {
        return clients.findAll().stream().map(mapper::client).toList();
    }

    public ClientDto get(Long id) {
        return mapper.client(client(id));
    }

    public List<ClientDto> search(String q) {
        return clients.search(q == null ? "" : q).stream().map(mapper::client).toList();
    }

    @Transactional
    public ClientDto create(ClientRequest request) {
        if (users.existsByEmail(request.email())) {
            throw ApiException.badRequest("Email уже занят");
        }
        User user = new User();
        user.setFullName(request.fullName());
        user.setEmail(request.email());
        user.setPhone(request.phone());
        user.setPasswordHash(encoder.encode(request.password() == null || request.password().isBlank() ? "client123" : request.password()));
        user.setRole(Role.CLIENT);
        users.save(user);
        Client client = new Client();
        client.setUser(user);
        client.setBirthDate(request.birthDate());
        client.setRfidCard(request.rfidCard());
        return mapper.client(clients.save(client));
    }

    @Transactional
    public ClientDto update(Long id, ClientRequest request) {
        Client client = client(id);
        User user = client.getUser();
        users.findByEmail(request.email()).ifPresent(existing -> {
            if (!existing.getId().equals(user.getId())) {
                throw ApiException.badRequest("Email уже занят");
            }
        });
        user.setFullName(request.fullName());
        user.setEmail(request.email());
        user.setPhone(request.phone());
        if (request.password() != null && !request.password().isBlank()) {
            user.setPasswordHash(encoder.encode(request.password()));
        }
        client.setBirthDate(request.birthDate());
        client.setRfidCard(request.rfidCard());
        return mapper.client(client);
    }

    @Transactional
    public void deactivate(Long id) {
        Client client = client(id);
        client.getUser().setEnabled(false);
    }

    public Client client(Long id) {
        return clients.findById(id).orElseThrow(() -> ApiException.notFound("Клиент не найден"));
    }
}
