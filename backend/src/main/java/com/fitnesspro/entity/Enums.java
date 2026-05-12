package com.fitnesspro.entity;

public final class Enums {
    private Enums() {
    }

    public enum Role { ADMIN, MANAGER, TRAINER, CLIENT }
    public enum MembershipStatus { ACTIVE, EXPIRED, FROZEN, CANCELLED, DEPLETED }
    public enum ScheduleStatus { PLANNED, COMPLETED, CANCELLED }
    public enum BookingStatus { ACTIVE, CANCELLED, ATTENDED, NO_SHOW }
    public enum VisitType { GYM, GROUP_TRAINING }
    public enum NotificationType { BOOKING, SCHEDULE, MEMBERSHIP, SYSTEM }
    public enum PaymentMethod { CASH, CARD, ONLINE }
    public enum SaleStatus { PAID, CANCELLED, REFUNDED }
}
