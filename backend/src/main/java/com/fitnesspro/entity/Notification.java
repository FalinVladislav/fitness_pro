package com.fitnesspro.entity;

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
    @Column(nullable = false)
    private boolean readStatus = false;
    @Column(nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();
}
