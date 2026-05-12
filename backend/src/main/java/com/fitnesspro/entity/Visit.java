package com.fitnesspro.entity;

import com.fitnesspro.entity.Enums.VisitType;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@Entity
public class Visit {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(optional = false)
    private Client client;
    @ManyToOne(optional = false)
    private Membership membership;
    @ManyToOne
    private Schedule schedule;
    @Column(nullable = false)
    private LocalDateTime visitTime = LocalDateTime.now();
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private VisitType visitType;
}
