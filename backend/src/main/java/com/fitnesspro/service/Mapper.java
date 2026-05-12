package com.fitnesspro.service;

import com.fitnesspro.dto.Dto.*;
import com.fitnesspro.entity.*;
import com.fitnesspro.entity.Enums.BookingStatus;
import com.fitnesspro.repository.BookingRepository;
import org.springframework.stereotype.Component;

@Component
public class Mapper {
    private final BookingRepository bookings;

    public Mapper(BookingRepository bookings) {
        this.bookings = bookings;
    }

    public UserDto user(User u) {
        return new UserDto(u.getId(), u.getFullName(), u.getEmail(), u.getPhone(), u.getRole(), u.isEnabled());
    }

    public ClientDto client(Client c) {
        return new ClientDto(c.getId(), user(c.getUser()), c.getRegistrationDate(), c.getRfidCard(), c.getBirthDate());
    }

    public TrainerDto trainer(Trainer t) {
        return new TrainerDto(t.getId(), user(t.getUser()), t.getSpecialization(), t.getDescription());
    }

    public MembershipTypeDto membershipType(MembershipType t) {
        return new MembershipTypeDto(t.getId(), t.getName(), t.getDurationDays(), t.getVisitCount(), t.getPrice(), t.getDescription(), t.isActive());
    }

    public MembershipDto membership(Membership m) {
        return new MembershipDto(m.getId(), m.getClient().getId(), m.getClient().getUser().getFullName(),
                m.getMembershipType().getId(), m.getMembershipType().getName(), m.getPurchaseDate(),
                m.getActivationDate(), m.getExpirationDate(), m.getRemainingVisits(), m.getStatus());
    }

    public HallDto hall(Hall h) {
        return new HallDto(h.getId(), h.getName(), h.getCapacity(), h.getDescription());
    }

    public TrainingTypeDto trainingType(TrainingType t) {
        return new TrainingTypeDto(t.getId(), t.getName(), t.getDurationMinutes(), t.getDescription(), t.getColor());
    }

    public ScheduleDto schedule(Schedule s) {
        long booked = bookings.countByScheduleAndStatus(s, BookingStatus.ACTIVE);
        return new ScheduleDto(s.getId(), s.getTrainingType().getId(), s.getTrainingType().getName(),
                s.getTrainer().getId(), s.getTrainer().getUser().getFullName(), s.getHall().getId(),
                s.getHall().getName(), s.getDate(), s.getStartTime(), s.getEndTime(), s.getParticipantLimit(),
                s.getStatus(), booked, s.getTrainerComment());
    }

    public BookingDto booking(Booking b) {
        return new BookingDto(b.getId(), b.getClient().getId(), b.getClient().getUser().getFullName(),
                schedule(b.getSchedule()), b.getBookingDateTime(), b.getStatus());
    }

    public VisitDto visit(Visit v) {
        Long scheduleId = v.getSchedule() == null ? null : v.getSchedule().getId();
        return new VisitDto(v.getId(), v.getClient().getId(), v.getClient().getUser().getFullName(),
                v.getMembership().getId(), scheduleId, v.getVisitTime(), v.getVisitType());
    }

    public NotificationDto notification(Notification n) {
        return new NotificationDto(n.getId(), n.getTitle(), n.getMessage(), n.getType(), n.isReadStatus(), n.getCreatedAt());
    }
}
