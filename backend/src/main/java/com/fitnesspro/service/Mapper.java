package com.fitnesspro.service;

import com.fitnesspro.dto.Dto.*;
import com.fitnesspro.entity.*;
import com.fitnesspro.entity.Enums.BookingStatus;
import com.fitnesspro.repository.BookingRepository;
import com.fitnesspro.repository.MembershipFreezeRepository;
import org.springframework.stereotype.Component;
@Component
public class Mapper {
    private final BookingRepository bookings;
    private final MembershipFreezeRepository freezes;

    public Mapper(BookingRepository bookings, MembershipFreezeRepository freezes) {
        this.bookings = bookings;
        this.freezes = freezes;
    }

    public UserDto user(User u) {
        return new UserDto(u.getId(), u.getFullName(), u.getEmail(), u.getPhone(), u.getRole(), u.isEnabled());
    }

    public ClientDto client(Client c) {
        return new ClientDto(c.getId(), user(c.getUser()), c.getRegistrationDate(), c.getRfidCard(), c.getBirthDate());
    }

    public TrainerDto trainer(Trainer t) {
        return new TrainerDto(t.getId(), user(t.getUser()), t.getSpecialization(), t.getYearsOfExperience(), t.getDescription());
    }

    public MembershipTypeDto membershipType(MembershipType t) {
        return new MembershipTypeDto(t.getId(), t.getName(), t.getDurationDays(), t.getVisitCount(),
                t.getPrice(), t.getDescription(), t.isActive(), t.isFreezeAllowed(), t.getMaxFreezeDays());
    }

    public MembershipDto membership(Membership m) {
        MembershipType type = m.getMembershipType();
        int used = freezes.totalFrozenDays(m);
        return new MembershipDto(m.getId(), m.getClient().getId(), m.getClient().getUser().getFullName(),
                type.getId(), type.getName(), m.getPurchaseDate(),
                m.getActivationDate(), m.getExpirationDate(), m.getRemainingVisits(), m.getStatus(),
                type.isFreezeAllowed(), type.getMaxFreezeDays(), used);
    }

    public MembershipFreezeDto freeze(MembershipFreeze f) {
        return new MembershipFreezeDto(f.getId(), f.getMembership().getId(),
                f.getStartDate(), f.getEndDate(), f.getReason(), f.getStatus());
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
        Long membershipId = v.getMembership() == null ? null : v.getMembership().getId();
        return new VisitDto(v.getId(), v.getClient().getId(), v.getClient().getUser().getFullName(),
                membershipId, scheduleId, v.getVisitTime(), v.getVisitType());
    }

    public NotificationDto notification(Notification n) {
        return new NotificationDto(n.getId(), n.getTitle(), n.getMessage(), n.getType(),
                n.getChannel(), n.getDeliveryStatus(), n.isReadStatus(), n.getCreatedAt(), n.getSentAt());
    }

    public MembershipPurchaseRequestDto purchaseRequest(MembershipPurchaseRequest r) {
        MembershipType type = r.getMembershipType();
        return new MembershipPurchaseRequestDto(
                r.getId(),
                r.getClient().getId(), r.getClient().getUser().getFullName(),
                type.getId(), type.getName(), type.getPrice(),
                r.getCreatedAt(), r.getDesiredActivationDate(), r.getComment(),
                r.getStatus(),
                r.getDecidedAt(),
                r.getDecidedBy() == null ? null : r.getDecidedBy().getFullName(),
                r.getDecisionComment(),
                r.getCreatedMembership() == null ? null : r.getCreatedMembership().getId()
        );
    }
}
