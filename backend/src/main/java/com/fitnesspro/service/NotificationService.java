package com.fitnesspro.service;

import com.fitnesspro.dto.Dto.NotificationDto;
import com.fitnesspro.entity.Booking;
import com.fitnesspro.entity.Client;
import com.fitnesspro.entity.Enums.BookingStatus;
import com.fitnesspro.entity.Enums.MembershipStatus;
import com.fitnesspro.entity.Enums.NotificationChannel;
import com.fitnesspro.entity.Enums.NotificationDeliveryStatus;
import com.fitnesspro.entity.Enums.NotificationType;
import com.fitnesspro.entity.Membership;
import com.fitnesspro.entity.Notification;
import com.fitnesspro.entity.User;
import com.fitnesspro.exception.ApiException;
import com.fitnesspro.repository.BookingRepository;
import com.fitnesspro.repository.ClientRepository;
import com.fitnesspro.repository.MembershipRepository;
import com.fitnesspro.repository.NotificationRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class NotificationService {
    private final NotificationRepository notifications;
    private final ClientRepository clients;
    private final BookingRepository bookings;
    private final MembershipRepository memberships;
    private final Mapper mapper;

    public NotificationService(NotificationRepository notifications, ClientRepository clients,
                               BookingRepository bookings, MembershipRepository memberships, Mapper mapper) {
        this.notifications = notifications;
        this.clients = clients;
        this.bookings = bookings;
        this.memberships = memberships;
        this.mapper = mapper;
    }

    @Transactional
    public Notification notify(User user, String title, String message, NotificationType type) {
        return notify(user, title, message, type, NotificationChannel.IN_APP);
    }

    @Transactional
    public Notification notify(User user, String title, String message, NotificationType type, NotificationChannel channel) {
        Notification n = new Notification();
        n.setUser(user);
        n.setTitle(title);
        n.setMessage(message);
        n.setType(type);
        n.setChannel(channel);
        // IN_APP считаем сразу доставленным, остальные каналы помечаем как SCHEDULED — фактическая отправка произойдет внешними интеграциями
        if (channel == NotificationChannel.IN_APP) {
            n.setDeliveryStatus(NotificationDeliveryStatus.SENT);
            n.setSentAt(LocalDateTime.now());
        } else {
            n.setDeliveryStatus(NotificationDeliveryStatus.SCHEDULED);
        }
        return notifications.save(n);
    }

    public List<NotificationDto> byUser(User user) {
        generateClientReminders(user);
        return notifications.findByUserOrderByCreatedAtDesc(user).stream().map(mapper::notification).toList();
    }

    @Transactional
    public void read(Long id, User user) {
        Notification n = notifications.findById(id).orElseThrow(() -> ApiException.notFound("Уведомление не найдено"));
        if (!n.getUser().getId().equals(user.getId())) {
            throw ApiException.forbidden("Нельзя читать чужое уведомление");
        }
        n.setReadStatus(true);
    }

    private void generateClientReminders(User user) {
        clients.findByUserEmail(user.getEmail()).ifPresent(client -> {
            LocalDateTime now = LocalDateTime.now();
            LocalDateTime until = now.plusHours(24);
            bookings.findByClientOrderByBookingDateTimeDesc(client).stream()
                    .filter(b -> b.getStatus() == BookingStatus.ACTIVE)
                    .filter(b -> {
                        LocalDateTime start = LocalDateTime.of(b.getSchedule().getDate(), b.getSchedule().getStartTime());
                        return !start.isBefore(now) && !start.isAfter(until);
                    })
                    .forEach(this::trainingReminder);
            memberships.findByClientOrderByActivationDateDesc(client).stream()
                    .filter(m -> m.getStatus() == MembershipStatus.ACTIVE)
                    .filter(m -> !m.getExpirationDate().isBefore(LocalDate.now())
                            && !m.getExpirationDate().isAfter(LocalDate.now().plusDays(7)))
                    .forEach(this::membershipExpirationReminder);
        });
    }

    private void trainingReminder(Booking booking) {
        String message = "Напоминание: " + booking.getSchedule().getTrainingType().getName() + " начнется "
                + booking.getSchedule().getDate() + " в " + booking.getSchedule().getStartTime() + ".";
        notifyOnce(booking.getClient().getUser(), "Напоминание о тренировке", message, NotificationType.SCHEDULE);
    }

    private void membershipExpirationReminder(Membership membership) {
        String message = "Абонемент \"" + membership.getMembershipType().getName() + "\" действует до "
                + membership.getExpirationDate() + ".";
        notifyOnce(membership.getClient().getUser(), "Абонемент скоро истекает", message, NotificationType.MEMBERSHIP);
    }

    private void notifyOnce(User user, String title, String message, NotificationType type) {
        if (!notifications.existsByUserAndTitleAndMessage(user, title, message)) {
            notify(user, title, message, type);
        }
    }
}
