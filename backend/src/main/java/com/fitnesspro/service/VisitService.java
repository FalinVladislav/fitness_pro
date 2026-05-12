package com.fitnesspro.service;

import com.fitnesspro.dto.Dto.*;
import com.fitnesspro.entity.*;
import com.fitnesspro.entity.Enums.*;
import com.fitnesspro.exception.ApiException;
import com.fitnesspro.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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

    @Transactional
    public VisitDto checkIn(CheckInRequest request) {
        Client client = clients.findById(request.clientId()).orElseThrow(() -> ApiException.notFound("Клиент не найден"));
        Membership membership = membershipService.activeFor(client);
        Schedule schedule = request.scheduleId() == null ? null : schedules.findById(request.scheduleId()).orElseThrow(() -> ApiException.notFound("Занятие не найдено"));
        membershipService.chargeVisit(membership);

        Visit visit = new Visit();
        visit.setClient(client);
        visit.setMembership(membership);
        visit.setSchedule(schedule);
        visit.setVisitType(schedule == null ? VisitType.GYM : VisitType.GROUP_TRAINING);
        visits.save(visit);

        if (schedule != null) {
            bookings.findByClientAndSchedule(client, schedule).ifPresent(b -> b.setStatus(BookingStatus.ATTENDED));
        }
        return mapper.visit(visit);
    }
}
