package com.fitnesspro.controller;

import com.fitnesspro.dto.Dto.*;
import com.fitnesspro.service.AuthService;
import com.fitnesspro.service.MembershipService;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/memberships")
public class MembershipController {
    private final MembershipService memberships;
    private final AuthService auth;

    public MembershipController(MembershipService memberships, AuthService auth) {
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

    @PostMapping("/buy")
    @PreAuthorize("hasRole('CLIENT')")
    public MembershipDto buy(@Valid @RequestBody CreatePurchaseRequest request) {
        return memberships.buyForCurrentClient(request, auth.currentUser());
    }

    @PostMapping("/{id}/renew")
    @PreAuthorize("hasAnyRole('ADMIN','CLIENT')")
    public MembershipDto renew(@PathVariable Long id) {
        ensureOwnerOrStaff(id);
        return memberships.renew(id);
    }

    @PostMapping("/{id}/cancel")
    @PreAuthorize("hasRole('ADMIN')")
    public MembershipDto cancel(@PathVariable Long id) { return memberships.cancel(id); }

    @PostMapping("/{id}/freeze")
    @PreAuthorize("hasAnyRole('ADMIN','CLIENT')")
    public MembershipFreezeDto freeze(@PathVariable Long id, @Valid @RequestBody FreezeMembershipRequest request) {
        ensureOwnerOrStaff(id);
        return memberships.freeze(id, request);
    }

    @PostMapping("/{id}/unfreeze")
    @PreAuthorize("hasAnyRole('ADMIN','CLIENT')")
    public MembershipFreezeDto unfreeze(@PathVariable Long id) {
        ensureOwnerOrStaff(id);
        return memberships.unfreeze(id);
    }

    @GetMapping("/{id}/freezes")
    public List<MembershipFreezeDto> freezes(@PathVariable Long id) {
        ensureOwnerOrStaff(id);
        return memberships.freezesOf(id);
    }

    @GetMapping("/{id}/status")
    public MembershipDto status(@PathVariable Long id) {
        ensureOwnerOrStaff(id);
        return memberships.status(id);
    }

    private void ensureOwnerOrStaff(Long membershipId) {
        var user = auth.currentUser();
        var role = user.getRole();
        if (role == com.fitnesspro.entity.Enums.Role.ADMIN || role == com.fitnesspro.entity.Enums.Role.MANAGER) {
            return;
        }
        if (!memberships.isOwner(membershipId, user)) {
            throw com.fitnesspro.exception.ApiException.forbidden("Нет доступа к этому абонементу");
        }
    }
}
