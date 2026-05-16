package com.fitnesspro.service;

import com.fitnesspro.dto.Dto.*;
import com.fitnesspro.entity.*;
import com.fitnesspro.entity.Enums.*;
import com.fitnesspro.exception.ApiException;
import com.fitnesspro.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
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
    public ScheduleDto save(Long id, ScheduleRequest r, User currentUser) {
        Schedule s = id == null ? new Schedule() : schedule(id);
        String previous = id == null ? null : describe(s);
        TrainingType type = trainingTypes.findById(r.trainingTypeId()).orElseThrow(() -> ApiException.notFound("Тип тренировки не найден"));
        Trainer trainer = resolveTrainer(r.trainerId(), currentUser);
        if (currentUser.getRole() == Role.TRAINER && id != null && !s.getTrainer().getUser().getId().equals(currentUser.getId())) {
            throw ApiException.forbidden("Тренер может изменять только свои занятия");
        }
        Hall hall = halls.findById(r.hallId()).orElseThrow(() -> ApiException.notFound("Зал не найден"));
        LocalTime end = r.endTime() == null ? r.startTime().plusMinutes(type.getDurationMinutes()) : r.endTime();
        if (!end.isAfter(r.startTime())) {
            throw ApiException.badRequest("Время окончания должно быть позже времени начала");
        }
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
        Schedule saved = schedules.save(s);
        if (id != null && previous != null && !previous.equals(describe(saved))) {
            bookings.findByScheduleAndStatus(saved, BookingStatus.ACTIVE).forEach(b ->
                    notifications.notify(b.getClient().getUser(), "Занятие изменено",
                            "Администратор или тренер изменил занятие. Новые данные: " + describe(saved),
                            NotificationType.SCHEDULE));
        }
        return mapper.schedule(saved);
    }

    @Transactional
    public void cancel(Long id, User currentUser) {
        Schedule s = schedule(id);
        if (currentUser.getRole() == Role.TRAINER && !s.getTrainer().getUser().getId().equals(currentUser.getId())) {
            throw ApiException.forbidden("Тренер может отменять только свои занятия");
        }
        if (s.getStatus() == ScheduleStatus.COMPLETED) {
            throw ApiException.badRequest("Проведённое занятие нельзя отменить");
        }
        s.setStatus(ScheduleStatus.CANCELLED);
        bookings.findByScheduleAndStatus(s, BookingStatus.ACTIVE).forEach(b -> {
            b.setStatus(BookingStatus.CANCELLED);
            notifications.notify(b.getClient().getUser(), "Занятие отменено",
                    "Занятие " + s.getTrainingType().getName() + " " + s.getDate() + " отменено.", NotificationType.SCHEDULE);
        });
    }

    @Transactional
    public ScheduleDto complete(Long id, User currentUser) {
        Schedule s = schedule(id);
        if (currentUser.getRole() == Role.TRAINER && !s.getTrainer().getUser().getId().equals(currentUser.getId())) {
            throw ApiException.forbidden("Тренер может завершать только свои занятия");
        }
        if (s.getStatus() == ScheduleStatus.CANCELLED) {
            throw ApiException.badRequest("Нельзя завершить отменённое занятие");
        }
        if (s.getStatus() == ScheduleStatus.COMPLETED) {
            throw ApiException.badRequest("Занятие уже отмечено как проведённое");
        }
        if (LocalDateTime.of(s.getDate(), s.getStartTime()).isAfter(LocalDateTime.now())) {
            throw ApiException.badRequest("Нельзя завершить занятие до его начала");
        }
        s.setStatus(ScheduleStatus.COMPLETED);
        // помечаем активные брони как NO_SHOW (не пришли). ATTENDED уже выставлены в VisitService.
        bookings.findByScheduleAndStatus(s, BookingStatus.ACTIVE).forEach(b -> b.setStatus(BookingStatus.NO_SHOW));
        notifications.notify(s.getTrainer().getUser(), "Занятие проведено",
                "Занятие " + s.getTrainingType().getName() + " " + s.getDate() + " отмечено как проведённое.",
                NotificationType.SCHEDULE);
        return mapper.schedule(s);
    }

    @Transactional
    public void delete(Long id, User currentUser) {
        Schedule s = schedule(id);
        if (s.getStatus() == ScheduleStatus.COMPLETED) {
            throw ApiException.badRequest("Проведённое занятие нельзя удалить из истории");
        }
        cancel(id, currentUser);
        schedules.deleteById(id);
    }

    public Schedule schedule(Long id) {
        return schedules.findById(id).orElseThrow(() -> ApiException.notFound("Занятие не найдено"));
    }

    private String describe(Schedule s) {
        return s.getTrainingType().getName() + " " + s.getDate() + " " + s.getStartTime() + "-" + s.getEndTime()
                + ", " + s.getHall().getName() + ", тренер " + s.getTrainer().getUser().getFullName();
    }

    private Trainer resolveTrainer(Long trainerId, User currentUser) {
        if (currentUser.getRole() == Role.TRAINER) {
            return trainers.findByUserEmail(currentUser.getEmail()).orElseThrow(() -> ApiException.forbidden("Профиль тренера не найден"));
        }
        if (trainerId == null) {
            throw ApiException.badRequest("Нужно выбрать тренера");
        }
        return trainers.findById(trainerId).orElseThrow(() -> ApiException.notFound("Тренер не найден"));
    }
}
