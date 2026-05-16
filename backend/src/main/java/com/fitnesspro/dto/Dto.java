package com.fitnesspro.dto;

import com.fitnesspro.entity.Enums.*;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

public final class Dto {
    private Dto() {
    }

    public record LoginRequest(@Email String email, @NotBlank String password) {}
    public record RegisterRequest(@NotBlank String fullName, @Email String email, @NotBlank String phone,
                                  @NotBlank String password, LocalDate birthDate) {}
    public record AuthResponse(String token, UserDto user) {}
    public record UserDto(Long id, String fullName, String email, String phone, Role role, boolean enabled) {}

    public record ClientDto(Long id, UserDto user, LocalDate registrationDate, String rfidCard, LocalDate birthDate) {}
    public record ClientRequest(@NotBlank String fullName, @Email String email, @NotBlank String phone,
                                String password, String rfidCard, LocalDate birthDate) {}

    public record TrainerDto(Long id, UserDto user, String specialization, Integer yearsOfExperience, String description) {}
    public record TrainerRequest(@NotBlank String fullName, @Email String email, @NotBlank String phone,
                                 String password, String specialization,
                                 @PositiveOrZero Integer yearsOfExperience, String description) {}

    public record MembershipTypeDto(Long id, String name, Integer durationDays, Integer visitCount,
                                    BigDecimal price, String description, boolean active,
                                    boolean freezeAllowed, Integer maxFreezeDays) {}
    public record MembershipTypeRequest(@NotBlank String name, @Positive Integer durationDays,
                                        @PositiveOrZero Integer visitCount, @Positive BigDecimal price,
                                        String description, boolean active,
                                        boolean freezeAllowed,
                                        @PositiveOrZero Integer maxFreezeDays) {}

    public record MembershipDto(Long id, Long clientId, String clientName, Long membershipTypeId, String typeName,
                                LocalDate purchaseDate, LocalDate activationDate, LocalDate expirationDate,
                                Integer remainingVisits, MembershipStatus status,
                                boolean freezeAllowed, Integer maxFreezeDays, Integer usedFreezeDays) {}
    public record SellMembershipRequest(@NotNull Long clientId, @NotNull Long membershipTypeId,
                                        LocalDate activationDate, PaymentMethod paymentMethod) {}
    public record FreezeMembershipRequest(@NotNull LocalDate startDate, @NotNull LocalDate endDate, String reason) {}
    public record MembershipFreezeDto(Long id, Long membershipId, LocalDate startDate, LocalDate endDate,
                                      String reason, MembershipFreezeStatus status) {}

    public record CreatePurchaseRequest(@NotNull Long membershipTypeId, LocalDate desiredActivationDate, String comment) {}
    public record DecidePurchaseRequest(String comment) {}
    public record MembershipPurchaseRequestDto(Long id, Long clientId, String clientName,
                                               Long membershipTypeId, String membershipTypeName, java.math.BigDecimal price,
                                               LocalDateTime createdAt, LocalDate desiredActivationDate, String comment,
                                               PurchaseRequestStatus status,
                                               LocalDateTime decidedAt, String decisionBy, String decisionComment,
                                               Long createdMembershipId) {}

    public record HallDto(Long id, String name, Integer capacity, String description) {}
    public record HallRequest(@NotBlank String name, @Positive Integer capacity, String description) {}

    public record TrainingTypeDto(Long id, String name, Integer durationMinutes, String description, String color) {}
    public record TrainingTypeRequest(@NotBlank String name, @Positive Integer durationMinutes,
                                      String description, String color) {}

    public record ScheduleDto(Long id, Long trainingTypeId, String trainingTypeName, Long trainerId, String trainerName,
                              Long hallId, String hallName, LocalDate date, LocalTime startTime, LocalTime endTime,
                              Integer participantLimit, ScheduleStatus status, long bookedCount, String trainerComment) {}
    public record ScheduleRequest(@NotNull Long trainingTypeId, Long trainerId, @NotNull Long hallId,
                                  @NotNull LocalDate date, @NotNull LocalTime startTime, LocalTime endTime,
                                  @Positive Integer participantLimit, String trainerComment) {}

    public record BookingDto(Long id, Long clientId, String clientName, ScheduleDto schedule,
                             LocalDateTime bookingDateTime, BookingStatus status) {}
    public record BookingRequest(@NotNull Long scheduleId, Long clientId) {}

    public record VisitDto(Long id, Long clientId, String clientName, Long membershipId, Long scheduleId,
                           LocalDateTime visitTime, VisitType visitType) {}
    public record CheckInRequest(@NotNull Long clientId, Long scheduleId, VisitType visitType) {}

    public record NotificationDto(Long id, String title, String message, NotificationType type,
                                  NotificationChannel channel, NotificationDeliveryStatus deliveryStatus,
                                  boolean readStatus, LocalDateTime createdAt, LocalDateTime sentAt) {}
    public record ReportValue(String label, Number value) {}
}
