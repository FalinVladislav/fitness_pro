package com.fitnesspro.controller;

import com.fitnesspro.dto.Dto.*;
import com.fitnesspro.service.ClientService;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/clients")
public class ClientController {
    private final ClientService clients;

    public ClientController(ClientService clients) {
        this.clients = clients;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    public List<ClientDto> all() {
        return clients.all();
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    public ClientDto get(@PathVariable Long id) {
        return clients.get(id);
    }

    @GetMapping("/search")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    public List<ClientDto> search(@RequestParam(required = false) String q) {
        return clients.search(q);
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ClientDto create(@Valid @RequestBody ClientRequest request) {
        return clients.create(request);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ClientDto update(@PathVariable Long id, @Valid @RequestBody ClientRequest request) {
        return clients.update(id, request);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public void deactivate(@PathVariable Long id) {
        clients.deactivate(id);
    }
}
