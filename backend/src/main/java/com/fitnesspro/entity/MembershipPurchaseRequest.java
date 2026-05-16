package com.fitnesspro.entity;

import com.fitnesspro.entity.Enums.PurchaseRequestStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Setter
@Entity
public class MembershipPurchaseRequest {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    private Client client;

    @ManyToOne(optional = false)
    private MembershipType membershipType;

    @Column(nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    private LocalDate desiredActivationDate;

    @Column(length = 500)
    private String comment;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PurchaseRequestStatus status = PurchaseRequestStatus.PENDING;

    @ManyToOne
    private User decidedBy;

    private LocalDateTime decidedAt;

    @Column(length = 500)
    private String decisionComment;

    @ManyToOne
    private Membership createdMembership;
}
