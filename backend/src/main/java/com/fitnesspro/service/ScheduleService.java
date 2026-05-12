package com.fitnesspro.service;

import com.fitnesspro.dto.Dto.*;
import com.fitnesspro.entity.*;
import com.fitnesspro.entity.Enums.*;
import com.fitnesspro.exception.ApiException;
import com.fitnesspro.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@Service
public class ScheduleService {
    private final ScheduleRepository schedules;
    private final TrainingTypeRepository trainingTypes;
    private final TrainerRepository trainers;
    private final HallRepository halls;
    private final BookingRepository bookings;
    private final NotificationService notifications;
    private final Mapper mapper;

    public ScheduleService(ScheduleRepository schedules, TrainingTypeRepository trainingTypes, TrainerRepository trainers,
                           HallRepository halls, BookingRepository bookings, NotificationService notifications, Mapper mapper) {
        this.schedules = schedules;
        this.trainingTypes = trainingTypes;
        this.trainers = trainers;
        this.halls = halls;
        this.bookings = bookings;
        this.notifications = notifications;
        this.mapper = mapper;
    }

    public List<ScheduleDto> list(LocalDate from, LocalDate to) {
        LocalDate start = from == null ? LocalDate.now().minusDays(7) : from;
        LocalDate end = to == null ? LocalDate.now().plusDays(30) : to;
        return schedules.findByDateBetweenOrderByDateAscStartTimeAsc(start, end).stream().map(mapper::schedule).toList();
    }

    public ScheduleDto get(Long id) {
        return mapper.schedule(schedule(id));
    }

    public List<ScheduleDto> byTrainer(Long trainerId) {
        Trainer trainer = trainers.findById(trainerId).orElseThrow(() -> ApiException.notFound("Тренер не найден"));
        return schedules.findByTrainerOrderByDateAscStartTimeAsc(trainer).stream().map(mapper::schedule).toList();
    }

    public List<ScheduleDto> myTrainerSchedule(String email) {
        Trainer trainer = trainers.findByUserEmail(email).orElseThrow(() -> ApiException.forbidden("Профиль тренера не найден"));
        return byTrainer(trainer.getId());
    }

    @Transactional
    public ScheduleDto save(Long id, ScheduleRequest r) {
        Schedule s = id == null ? new Schedule() : schedule(id);
        TrainingType type = trainingTypes.findById(r.trainingTypeId()).orElseThrow(() -> ApiException.notFound("Тип тренировки не найден"));
        Trainer trainer = trainers.findById(r.trainerId()).orElseThrow(() -> ApiException.notFound("Тренер не найден"));
        Hall hall = halls.findById(r.hallId()).orElseThrow(() -> ApiException.notFound("Зал не найден"));
        LocalTime end = r.startTime().plusMinutes(type.getDurationMinutes());
        Long ignore = id == null ? -1L : id;
        if (schedules.trainerHasConflict(trainer.getId(), r.date(), r.startTime(), end, ignore)) {
            throw ApiException.badRequest("Тренер уже занят в это время");
        }
        if (schedules.hallHasConflict(hall.getId(), r.date(), r.startTime(), end, ignore)) {
            throw ApiException.badRequest("Зал уже занят в это время");
        }
        if (r.participantLimit() > hall.getCapacity()) {
            throw ApiException.badRequest("Лимит участников превышает вместимость зала");
        }
        s.setTrainingType(type);
        s.setTrainer(trainer);
        s.setHall(hall);
        s.setDate(r.date());
        s.setStartTime(r.startTime());
        s.setEndTime(end);
        s.setParticipantLimit(r.participantLimit());
        s.setTrainerComment(r.trainerComment());
        return mapper.schedule(schedules.save(s));
    }

    @Transactional
    public void cancel(Long id) {
        Schedule s = schedule(id);
        s.setStatus(ScheduleStatus.CANCELLED);
        bookings.findByScheduleAndStatus(s, BookingStatus.ACTIVE).forEach(b -> {
            b.setStatus(BookingStatus.CANCELLED);
            notifications.notify(b.getClient().getUser(), "Занятие отменено",
                    "Занятие " + s.getTrainingType().getName() + " " + s.getDate() + " отменено.", NotificationType.SCHEDULE);
        });
    }

    @Transactional
    public void delete(Long id) {
        cancel(id);
        schedules.deleteById(id);
    }

    public Schedule schedule(Long id) {
        return schedules.findById(id).orElseThrow(() -> ApiException.notFound("Занятие не найдено"));
    }
}
