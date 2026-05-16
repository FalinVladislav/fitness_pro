package com.fitnesspro.controller;

import com.fitnesspro.dto.Dto.*;
import com.fitnesspro.service.AuthService;
import com.fitnesspro.service.MembershipPurchaseRequestService;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/membership-requests")
public class MembershipPurchaseRequestController {
    private final MembershipPurchaseRequestService service;
    private final AuthService auth;

    public MembershipPurchaseRequestController(MembershipPurchaseRequestService service, AuthService auth) {
        this.service = service;
        this.auth = auth;
    }

    @PostMapping
    @PreAuthorize("hasRole('CLIENT')")
    public MembershipPurchaseRequestDto create(@Valid @RequestBody CreatePurchaseRequest body) {
        return service.create(body, auth.currentUser());
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    public List<MembershipPurchaseRequestDto> all(@RequestParam(required = false) Boolean pending) {
        return Boolean.TRUE.equals(pending) ? service.pending() : service.all();
    }

    @GetMapping("/my")
    @PreAuthorize("hasRole('CLIENT')")
    public List<MembershipPurchaseRequestDto> my() {
        return service.my(auth.currentUser());
    }

    @PostMapping("/{id}/approve")
    @PreAuthorize("hasRole('ADMIN')")
    public MembershipPurchaseRequestDto approve(@PathVariable Long id, @RequestBody(required = false) DecidePurchaseRequest body) {
        return service.approve(id, body, auth.currentUser());
    }

    @PostMapping("/{id}/reject")
    @PreAuthorize("hasRole('ADMIN')")
    public MembershipPurchaseRequestDto reject(@PathVariable Long id, @RequestBody(required = false) DecidePurchaseRequest body) {
        return service.reject(id, body, auth.currentUser());
    }

    @PostMapping("/{id}/cancel")
    @PreAuthorize("hasRole('CLIENT')")
    public MembershipPurchaseRequestDto cancel(@PathVariable Long id) {
        return service.cancel(id, auth.currentUser());
    }
}
