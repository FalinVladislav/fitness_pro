package com.fitnesspro.entity;

import com.fitnesspro.entity.Enums.BookingStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@Entity
@Table(uniqueConstraints = @UniqueConstraint(columnNames = {"client_id", "schedule_id"}))
public class Booking {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(optional = false)
    private Client client;
    @ManyToOne(optional = false)
    private Schedule schedule;
    @Column(nullable = false)
    private LocalDateTime bookingDateTime = LocalDateTime.now();
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private BookingStatus status = BookingStatus.ACTIVE;
}
