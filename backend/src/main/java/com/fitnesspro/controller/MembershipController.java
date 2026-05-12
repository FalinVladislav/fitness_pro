package com.fitnesspro.controller;

import com.fitnesspro.dto.Dto.*;
import com.fitnesspro.service.MembershipService;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/memberships")
public class MembershipController {
    private final MembershipService memberships;
    private final com.fitnesspro.service.AuthService auth;

    public MembershipController(MembershipService memberships, com.fitnesspro.service.AuthService auth) {
        this.memberships = memberships;
        this.auth = auth;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    public List<MembershipDto> all() { return memberships.all(); }

    @GetMapping("/client/{clientId}")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    public List<MembershipDto> byClient(@PathVariable Long clientId) { return memberships.byClient(clientId); }

    @GetMapping("/my")
    @PreAuthorize("hasRole('CLIENT')")
    public List<MembershipDto> my() { return memberships.byCurrentClient(auth.currentUser()); }

    @PostMapping("/sell")
    @PreAuthorize("hasRole('ADMIN')")
    public MembershipDto sell(@Valid @RequestBody SellMembershipRequest request) { return memberships.sell(request); }

    @PostMapping("/{id}/renew")
    @PreAuthorize("hasAnyRole('ADMIN','CLIENT')")
    public MembershipDto renew(@PathVariable Long id) { return memberships.renew(id); }

    @GetMapping("/{id}/status")
    public MembershipDto status(@PathVariable Long id) { return memberships.status(id); }
}
