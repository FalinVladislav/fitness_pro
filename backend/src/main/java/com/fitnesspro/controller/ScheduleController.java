package com.fitnesspro.controller;

import com.fitnesspro.dto.Dto.*;
import com.fitnesspro.service.AuthService;
import com.fitnesspro.service.ScheduleService;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/schedule")
public class ScheduleController {
    private final ScheduleService schedules;
    private final AuthService auth;

    public ScheduleController(ScheduleService schedules, AuthService auth) {
        this.schedules = schedules;
        this.auth = auth;
    }

    @GetMapping
    public List<ScheduleDto> list(@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
                                  @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return schedules.list(from, to);
    }

    @GetMapping("/{id}")
    public ScheduleDto get(@PathVariable Long id) { return schedules.get(id); }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','TRAINER')")
    public ScheduleDto create(@Valid @RequestBody ScheduleRequest request) { return schedules.save(null, request, auth.currentUser()); }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','TRAINER')")
    public ScheduleDto update(@PathVariable Long id, @Valid @RequestBody ScheduleRequest request) { return schedules.save(id, request, auth.currentUser()); }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','TRAINER')")
    public void delete(@PathVariable Long id) { schedules.delete(id, auth.currentUser()); }

    @PostMapping("/{id}/cancel")
    @PreAuthorize("hasAnyRole('ADMIN','TRAINER')")
    public void cancel(@PathVariable Long id) { schedules.cancel(id, auth.currentUser()); }

    @PostMapping("/{id}/complete")
    @PreAuthorize("hasAnyRole('ADMIN','TRAINER')")
    public ScheduleDto complete(@PathVariable Long id) { return schedules.complete(id, auth.currentUser()); }

    @GetMapping("/trainer/{trainerId}")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER','TRAINER')")
    public List<ScheduleDto> byTrainer(@PathVariable Long trainerId) { return schedules.byTrainer(trainerId); }

    @GetMapping("/my")
    @PreAuthorize("hasRole('TRAINER')")
    public List<ScheduleDto> myTrainerSchedule() { return schedules.myTrainerSchedule(auth.currentUser().getEmail()); }
}
