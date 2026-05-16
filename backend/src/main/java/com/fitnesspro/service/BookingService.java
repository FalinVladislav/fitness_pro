package com.fitnesspro.service;

import com.fitnesspro.dto.Dto.*;
import com.fitnesspro.entity.*;
import com.fitnesspro.entity.Enums.*;
import com.fitnesspro.exception.ApiException;
import com.fitnesspro.repository.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class BookingService {
    private final BookingRepository bookings;
    private final ClientRepository clients;
    private final ScheduleService scheduleService;
    private final MembershipService membershipService;
    private final NotificationService notifications;
    private final Mapper mapper;
    private final long cancelDeadlineMinutes;

    public BookingService(BookingRepository bookings, ClientRepository clients, ScheduleService scheduleService,
                          MembershipService membershipService, NotificationService notifications, Mapper mapper,
                          @Value("${app.booking.cancel-deadline-minutes}") long cancelDeadlineMinutes) {
        this.bookings = bookings;
        this.clients = clients;
        this.scheduleService = scheduleService;
        this.membershipService = membershipService;
        this.notifications = notifications;
        this.mapper = mapper;
        this.cancelDeadlineMinutes = cancelDeadlineMinutes;
    }

    public List<BookingDto> all() {
        return bookings.findAll().stream().map(mapper::booking).toList();
    }

    public List<BookingDto> bySchedule(Long scheduleId, User currentUser) {
        Schedule schedule = scheduleService.schedule(scheduleId);
        if (currentUser.getRole() == Role.TRAINER && !schedule.getTrainer().getUser().getId().equals(currentUser.getId())) {
            throw ApiException.forbidden("Тренер видит участников только своих занятий");
        }
        return bookings.findBySchedule(schedule).stream().map(mapper::booking).toList();
    }

    public List<BookingDto> my(User user) {
        Client client = clients.findByUserEmail(user.getEmail()).orElseThrow(() -> ApiException.forbidden("Профиль клиента не найден"));
        return bookings.findByClientOrderByBookingDateTimeDesc(client).stream().map(mapper::booking).toList();
    }

    @Transactional
    public BookingDto create(BookingRequest request, User currentUser) {
        Client client = resolveClient(request.clientId(), currentUser);
        Schedule schedule = scheduleService.schedule(request.scheduleId());
        if (schedule.getStatus() == ScheduleStatus.CANCELLED) {
            throw ApiException.badRequest("Нельзя записаться на отмененную тренировку");
        }
        membershipService.activeFor(client);
        bookings.findByClientAndSchedule(client, schedule).ifPresent(existing -> {
            if (existing.getStatus() == BookingStatus.ACTIVE) {
                throw ApiException.badRequest("Клиент уже записан на это занятие");
            }
        });
        long booked = bookings.countByScheduleAndStatus(schedule, BookingStatus.ACTIVE);
        if (booked >= schedule.getParticipantLimit()) {
            throw ApiException.badRequest("На занятии нет свободных мест");
        }
        var existing = bookings.findByClientAndSchedule(client, schedule);
        if (existing.isPresent()) {
            Booking booking = existing.get();
            booking.setStatus(BookingStatus.ACTIVE);
            booking.setBookingDateTime(LocalDateTime.now());
            notifications.notify(client.getUser(), "Запись восстановлена",
                    "Вы снова записаны на " + schedule.getTrainingType().getName() + " " + schedule.getDate() + " в " + schedule.getStartTime(),
                    NotificationType.BOOKING);
            notifications.notify(schedule.getTrainer().getUser(), "Запись восстановлена",
                    client.getUser().getFullName() + " снова записался на ваше занятие.", NotificationType.BOOKING);
            return mapper.booking(booking);
        }
        Booking booking = new Booking();
        booking.setClient(client);
        booking.setSchedule(schedule);
        bookings.save(booking);
        notifications.notify(client.getUser(), "Запись подтверждена",
                "Вы записаны на " + schedule.getTrainingType().getName() + " " + schedule.getDate() + " в " + schedule.getStartTime(),
                NotificationType.BOOKING);
        notifications.notify(schedule.getTrainer().getUser(), "Новая запись",
                client.getUser().getFullName() + " записался на ваше занятие.", NotificationType.BOOKING);
        return mapper.booking(booking);
    }

    @Transactional
    public void cancel(Long id, User currentUser) {
        Booking booking = bookings.findById(id).orElseThrow(() -> ApiException.notFound("Запись не найдена"));
        boolean ownBooking = booking.getClient().getUser().getId().equals(currentUser.getId());
        boolean staff = currentUser.getRole() == Role.ADMIN || currentUser.getRole() == Role.MANAGER;
        if (!ownBooking && !staff) {
            throw ApiException.forbidden("Нельзя отменить чужую запись");
        }
        LocalDateTime start = LocalDateTime.of(booking.getSchedule().getDate(), booking.getSchedule().getStartTime());
        if (ownBooking && LocalDateTime.now().plusMinutes(cancelDeadlineMinutes).isAfter(start)) {
            throw ApiException.badRequest("Отмена уже недоступна: до занятия осталось слишком мало времени");
        }
        booking.setStatus(BookingStatus.CANCELLED);
        notifications.notify(booking.getClient().getUser(), "Запись отменена",
                "Запись на " + booking.getSchedule().getTrainingType().getName() + " "
                        + booking.getSchedule().getDate() + " в " + booking.getSchedule().getStartTime() + " отменена.",
                NotificationType.BOOKING);
        notifications.notify(booking.getSchedule().getTrainer().getUser(), "Отмена записи",
                booking.getClient().getUser().getFullName() + " отменил запись.", NotificationType.BOOKING);
    }

    private Client resolveClient(Long requestedClientId, User currentUser) {
        if (currentUser.getRole() == Role.CLIENT) {
            return clients.findByUserEmail(currentUser.getEmail()).orElseThrow(() -> ApiException.forbidden("Профиль клиента не найден"));
        }
        if (requestedClientId == null) {
            throw ApiException.badRequest("Для записи администратором нужно указать клиента");
        }
        return clients.findById(requestedClientId).orElseThrow(() -> ApiException.notFound("Клиент не найден"));
    }
}
