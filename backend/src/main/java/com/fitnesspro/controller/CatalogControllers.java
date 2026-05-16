package com.fitnesspro.controller;

import com.fitnesspro.dto.Dto.*;
import com.fitnesspro.service.CatalogService;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

public final class CatalogControllers {
    private CatalogControllers() {
    }

    @RestController
    @RequestMapping("/api/membership-types")
    public static class MembershipTypeController {
        private final CatalogService catalog;
        public MembershipTypeController(CatalogService catalog) { this.catalog = catalog; }

        @GetMapping public List<MembershipTypeDto> list() { return catalog.membershipTypes(); }
        @PostMapping @PreAuthorize("hasRole('ADMIN')") public MembershipTypeDto create(@Valid @RequestBody MembershipTypeRequest r) { return catalog.saveMembershipType(null, r); }
        @PutMapping("/{id}") @PreAuthorize("hasRole('ADMIN')") public MembershipTypeDto update(@PathVariable Long id, @Valid @RequestBody MembershipTypeRequest r) { return catalog.saveMembershipType(id, r); }
        @DeleteMapping("/{id}") @PreAuthorize("hasRole('ADMIN')") public void delete(@PathVariable Long id) { catalog.deleteMembershipType(id); }
    }

    @RestController
    @RequestMapping("/api/halls")
    public static class HallController {
        private final CatalogService catalog;
        public HallController(CatalogService catalog) { this.catalog = catalog; }

        @GetMapping public List<HallDto> list() { return catalog.halls(); }
        @PostMapping @PreAuthorize("hasRole('ADMIN')") public HallDto create(@Valid @RequestBody HallRequest r) { return catalog.saveHall(null, r); }
        @PutMapping("/{id}") @PreAuthorize("hasRole('ADMIN')") public HallDto update(@PathVariable Long id, @Valid @RequestBody HallRequest r) { return catalog.saveHall(id, r); }
        @DeleteMapping("/{id}") @PreAuthorize("hasRole('ADMIN')") public void delete(@PathVariable Long id) { catalog.deleteHall(id); }
    }

    @RestController
    @RequestMapping("/api/training-types")
    public static class TrainingTypeController {
        private final CatalogService catalog;
        public TrainingTypeController(CatalogService catalog) { this.catalog = catalog; }

        @GetMapping public List<TrainingTypeDto> list() { return catalog.trainingTypes(); }
        @PostMapping @PreAuthorize("hasRole('ADMIN')") public TrainingTypeDto create(@Valid @RequestBody TrainingTypeRequest r) { return catalog.saveTrainingType(null, r); }
        @PutMapping("/{id}") @PreAuthorize("hasRole('ADMIN')") public TrainingTypeDto update(@PathVariable Long id, @Valid @RequestBody TrainingTypeRequest r) { return catalog.saveTrainingType(id, r); }
        @DeleteMapping("/{id}") @PreAuthorize("hasRole('ADMIN')") public void delete(@PathVariable Long id) { catalog.deleteTrainingType(id); }
    }

    @RestController
    @RequestMapping("/api/trainers")
    public static class TrainerController {
        private final CatalogService catalog;
        public TrainerController(CatalogService catalog) { this.catalog = catalog; }

        @GetMapping public List<TrainerDto> list() { return catalog.trainers(); }
        @PostMapping @PreAuthorize("hasRole('ADMIN')") public TrainerDto create(@Valid @RequestBody TrainerRequest r) { return catalog.saveTrainer(null, r); }
        @PutMapping("/{id}") @PreAuthorize("hasRole('ADMIN')") public TrainerDto update(@PathVariable Long id, @Valid @RequestBody TrainerRequest r) { return catalog.saveTrainer(id, r); }
        @DeleteMapping("/{id}") @PreAuthorize("hasRole('ADMIN')") public void delete(@PathVariable Long id) { catalog.deleteTrainer(id); }
    }
}
