package com.fitnesspro.service;

import com.fitnesspro.dto.Dto.*;
import com.fitnesspro.entity.*;
import com.fitnesspro.entity.Enums.Role;
import com.fitnesspro.exception.ApiException;
import com.fitnesspro.repository.*;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class CatalogService {
    private final MembershipTypeRepository membershipTypes;
    private final HallRepository halls;
    private final TrainingTypeRepository trainingTypes;
    private final TrainerRepository trainers;
    private final UserRepository users;
    private final PasswordEncoder encoder;
    private final Mapper mapper;

    public CatalogService(MembershipTypeRepository membershipTypes, HallRepository halls, TrainingTypeRepository trainingTypes,
                          TrainerRepository trainers, UserRepository users, PasswordEncoder encoder, Mapper mapper) {
        this.membershipTypes = membershipTypes;
        this.halls = halls;
        this.trainingTypes = trainingTypes;
        this.trainers = trainers;
        this.users = users;
        this.encoder = encoder;
        this.mapper = mapper;
    }

    public List<MembershipTypeDto> membershipTypes() { return membershipTypes.findAll().stream().map(mapper::membershipType).toList(); }
    public List<HallDto> halls() { return halls.findAll().stream().map(mapper::hall).toList(); }
    public List<TrainingTypeDto> trainingTypes() { return trainingTypes.findAll().stream().map(mapper::trainingType).toList(); }
    public List<TrainerDto> trainers() { return trainers.findAll().stream().map(mapper::trainer).toList(); }

    @Transactional
    public MembershipTypeDto saveMembershipType(Long id, MembershipTypeRequest r) {
        MembershipType t = id == null ? new MembershipType() : membershipTypes.findById(id).orElseThrow(() -> ApiException.notFound("Тип абонемента не найден"));
        t.setName(r.name());
        t.setDurationDays(r.durationDays());
        t.setVisitCount(r.visitCount());
        t.setPrice(r.price());
        t.setDescription(r.description());
        t.setActive(r.active());
        return mapper.membershipType(membershipTypes.save(t));
    }

    @Transactional
    public HallDto saveHall(Long id, HallRequest r) {
        Hall h = id == null ? new Hall() : halls.findById(id).orElseThrow(() -> ApiException.notFound("Зал не найден"));
        h.setName(r.name());
        h.setCapacity(r.capacity());
        h.setDescription(r.description());
        return mapper.hall(halls.save(h));
    }

    @Transactional
    public TrainingTypeDto saveTrainingType(Long id, TrainingTypeRequest r) {
        TrainingType t = id == null ? new TrainingType() : trainingTypes.findById(id).orElseThrow(() -> ApiException.notFound("Тип тренировки не найден"));
        t.setName(r.name());
        t.setDurationMinutes(r.durationMinutes());
        t.setDescription(r.description());
        t.setColor(r.color());
        return mapper.trainingType(trainingTypes.save(t));
    }

    @Transactional
    public TrainerDto saveTrainer(Long id, TrainerRequest r) {
        Trainer trainer = id == null ? new Trainer() : trainers.findById(id).orElseThrow(() -> ApiException.notFound("Тренер не найден"));
        User user = id == null ? new User() : trainer.getUser();
        if (id == null && users.existsByEmail(r.email())) {
            throw ApiException.badRequest("Email уже занят");
        }
        user.setFullName(r.fullName());
        user.setEmail(r.email());
        user.setPhone(r.phone());
        user.setRole(Role.TRAINER);
        if (id == null || (r.password() != null && !r.password().isBlank())) {
            user.setPasswordHash(encoder.encode(r.password() == null || r.password().isBlank() ? "trainer123" : r.password()));
        }
        users.save(user);
        trainer.setUser(user);
        trainer.setSpecialization(r.specialization());
        trainer.setDescription(r.description());
        return mapper.trainer(trainers.save(trainer));
    }

    public void deleteMembershipType(Long id) { membershipTypes.deleteById(id); }
    public void deleteHall(Long id) { halls.deleteById(id); }
    public void deleteTrainingType(Long id) { trainingTypes.deleteById(id); }
}
