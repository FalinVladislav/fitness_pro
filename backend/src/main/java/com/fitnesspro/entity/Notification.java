package com.fitnesspro.entity;

import com.fitnesspro.entity.Enums.NotificationChannel;
import com.fitnesspro.entity.Enums.NotificationDeliveryStatus;
import com.fitnesspro.entity.Enums.NotificationType;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@Entity
public class Notification {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(optional = false)
    private User user;
    @Column(nullable = false)
    private String title;
    @Column(nullable = false, length = 1500)
    private String message;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private NotificationType type;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private NotificationChannel channel = NotificationChannel.IN_APP;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private NotificationDeliveryStatus deliveryStatus = NotificationDeliveryStatus.SCHEDULED;
    @Column(nullable = false)
    private boolean readStatus = false;
    @Column(nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();
    private LocalDateTime sentAt;
}
