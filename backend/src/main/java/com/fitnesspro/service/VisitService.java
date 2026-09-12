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
import java.util.List;

@Service
public class VisitService {
    private final VisitRepository visits;
    private final ClientRepository clients;
    private final ScheduleRepository schedules;
    private final BookingRepository bookings;
    private final MembershipService membershipService;
    private final Mapper mapper;

    public VisitService(VisitRepository visits, ClientRepository clients, ScheduleRepository schedules,
                        BookingRepository bookings, MembershipService membershipService, Mapper mapper) {
        this.visits = visits;
        this.clients = clients;
        this.schedules = schedules;
        this.bookings = bookings;
        this.membershipService = membershipService;
        this.mapper = mapper;
    }

    public List<VisitDto> all() {
        return visits.findAll().stream().map(mapper::visit).toList();
    }

    public List<VisitDto> byClient(Long clientId) {
        Client client = clients.findById(clientId).orElseThrow(() -> ApiException.notFound("Клиент не найден"));
        return visits.findByClientOrderByVisitTimeDesc(client).stream().map(mapper::visit).toList();
    }

    public List<VisitDto> my(String email) {
        Client client = clients.findByUserEmail(email).orElseThrow(() -> ApiException.forbidden("Профиль клиента не найден"));
        return visits.findByClientOrderByVisitTimeDesc(client).stream().map(mapper::visit).toList();
    }

    public List<VisitDto> byTrainer(String email) {
        return visits.findByScheduleTrainerUserEmailOrderByVisitTimeDesc(email).stream().map(mapper::visit).toList();
    }

    @Transactional
    public VisitDto checkIn(CheckInRequest request, User currentUser) {
        Client client = clients.findById(request.clientId()).orElseThrow(() -> ApiException.notFound("Клиент не найден"));
        Schedule schedule = resolveSchedule(request.scheduleId(), currentUser);
        VisitType type = resolveType(request.visitType(), schedule);
        ensureCanCheckIn(client, schedule);

        Membership membership = null;
        if (type != VisitType.ONE_TIME) {
            membership = membershipService.activeFor(client);
            membershipService.chargeVisit(membership);
        }

        Visit visit = new Visit();
        visit.setClient(client);
        visit.setMembership(membership);
        visit.setSchedule(schedule);
        visit.setVisitType(type);
        visits.save(visit);

        if (schedule != null) {
            bookings.findByClientAndSchedule(client, schedule).ifPresent(b -> b.setStatus(BookingStatus.ATTENDED));
        }
        return mapper.visit(visit);
    }

    @Transactional
    public void cancel(Long id) {
        Visit visit = visits.findById(id).orElseThrow(() -> ApiException.notFound("Посещение не найдено"));
        membershipService.restoreVisit(visit.getMembership());
        if (visit.getSchedule() != null) {
            bookings.findByClientAndSchedule(visit.getClient(), visit.getSchedule()).ifPresent(b -> b.setStatus(BookingStatus.ACTIVE));
        }
        visits.delete(visit);
    }

    private VisitType resolveType(VisitType requested, Schedule schedule) {
        if (schedule != null) {
            return VisitType.GROUP_TRAINING;
        }
        if (requested == VisitType.ONE_TIME) {
            return VisitType.ONE_TIME;
        }
        return VisitType.GYM;
    }

    private Schedule resolveSchedule(Long scheduleId, User currentUser) {
        if (currentUser.getRole() == Role.ADMIN) {
            if (scheduleId != null) {
                throw ApiException.forbidden("Администратор отмечает только посещение зала, групповые тренировки отмечает тренер");
            }
            return null;
        }
        if (currentUser.getRole() == Role.TRAINER) {
            if (scheduleId == null) {
                throw ApiException.badRequest("Для отметки тренером нужно выбрать занятие");
            }
            Schedule schedule = schedules.findById(scheduleId).orElseThrow(() -> ApiException.notFound("Занятие не найдено"));
            if (!schedule.getTrainer().getUser().getId().equals(currentUser.getId())) {
                throw ApiException.forbidden("Тренер может отмечать посещения только на своих занятиях");
            }
            return schedule;
        }
        throw ApiException.forbidden("Недостаточно прав для отметки посещения");
    }

    private void ensureCanCheckIn(Client client, Schedule schedule) {
        LocalDateTime now = LocalDateTime.now();
        if (schedule != null) {
            if (schedule.getStatus() != ScheduleStatus.PLANNED) {
                throw ApiException.badRequest("Посещение можно отметить только для запланированного занятия");
            }
            LocalDateTime start = LocalDateTime.of(schedule.getDate(), schedule.getStartTime());
            LocalDateTime end = LocalDateTime.of(schedule.getDate(), schedule.getEndTime());
            if (now.isBefore(start) || !now.isBefore(end)) {
                throw ApiException.badRequest("Посещение можно отметить только во время занятия");
            }
            Booking booking = bookings.findByClientAndSchedule(client, schedule)
                    .orElseThrow(() -> ApiException.badRequest("Клиент не записан на это занятие"));
            if (booking.getStatus() != BookingStatus.ACTIVE) {
                throw ApiException.badRequest("У клиента нет активной записи на это занятие");
            }
            if (visits.existsByClientAndSchedule(client, schedule)) {
                throw ApiException.badRequest("Посещение этого занятия уже отмечено");
            }
            return;
        }

        LocalDateTime dayStart = LocalDate.now().atStartOfDay();
        LocalDateTime nextDayStart = dayStart.plusDays(1);
        if (visits.existsByClientAndScheduleIsNullAndVisitTimeBetween(client, dayStart, nextDayStart)) {
            throw ApiException.badRequest("Посещение зала уже отмечено сегодня");
        }
    }
}
