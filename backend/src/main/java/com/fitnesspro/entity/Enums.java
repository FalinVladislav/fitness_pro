package com.fitnesspro.entity;

public final class Enums {
    private Enums() {
    }

    public enum Role { ADMIN, MANAGER, TRAINER, CLIENT }
    public enum MembershipStatus { ACTIVE, EXPIRED, FROZEN, CANCELLED, DEPLETED }
    public enum ScheduleStatus { PLANNED, COMPLETED, CANCELLED }
    public enum BookingStatus { ACTIVE, CANCELLED, ATTENDED, NO_SHOW }
    public enum VisitType { GYM, GROUP_TRAINING, ONE_TIME }
    public enum NotificationType { BOOKING, SCHEDULE, MEMBERSHIP, SYSTEM }
    public enum NotificationChannel { IN_APP, EMAIL, SMS, PUSH }
    public enum NotificationDeliveryStatus { SCHEDULED, SENT, FAILED }
    public enum PaymentMethod { CASH, CARD, ONLINE }
    public enum SaleStatus { PAID, CANCELLED, REFUNDED }
    public enum MembershipFreezeStatus { SCHEDULED, ACTIVE, FINISHED, CANCELLED }
    public enum PurchaseRequestStatus { PENDING, APPROVED, REJECTED, CANCELLED }
}
