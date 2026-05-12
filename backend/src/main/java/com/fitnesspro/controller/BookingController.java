package com.fitnesspro.controller;

import com.fitnesspro.dto.Dto.*;
import com.fitnesspro.service.AuthService;
import com.fitnesspro.service.BookingService;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/bookings")
public class BookingController {
    private final BookingService bookings;
    private final AuthService auth;

    public BookingController(BookingService bookings, AuthService auth) {
        this.bookings = bookings;
        this.auth = auth;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    public List<BookingDto> all() { return bookings.all(); }

    @GetMapping("/my")
    @PreAuthorize("hasRole('CLIENT')")
    public List<BookingDto> my() { return bookings.my(auth.currentUser()); }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','CLIENT')")
    public BookingDto create(@Valid @RequestBody BookingRequest request) { return bookings.create(request, auth.currentUser()); }

    @PostMapping("/{id}/cancel")
    @PreAuthorize("hasAnyRole('ADMIN','CLIENT')")
    public void cancel(@PathVariable Long id) { bookings.cancel(id, auth.currentUser()); }
}
