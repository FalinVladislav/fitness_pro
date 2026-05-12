package com.fitnesspro.controller;

import com.fitnesspro.dto.Dto.*;
import com.fitnesspro.service.VisitService;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/visits")
public class VisitController {
    private final VisitService visits;
    private final com.fitnesspro.service.AuthService auth;

    public VisitController(VisitService visits, com.fitnesspro.service.AuthService auth) {
        this.visits = visits;
        this.auth = auth;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    public List<VisitDto> all() { return visits.all(); }

    @GetMapping("/client/{clientId}")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    public List<VisitDto> byClient(@PathVariable Long clientId) { return visits.byClient(clientId); }

    @GetMapping("/my")
    @PreAuthorize("hasRole('CLIENT')")
    public List<VisitDto> my() { return visits.my(auth.currentUser().getEmail()); }

    @PostMapping("/check-in")
    @PreAuthorize("hasAnyRole('ADMIN','TRAINER')")
    public VisitDto checkIn(@Valid @RequestBody CheckInRequest request) { return visits.checkIn(request); }
}
