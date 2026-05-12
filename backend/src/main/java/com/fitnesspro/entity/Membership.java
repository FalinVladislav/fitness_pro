package com.fitnesspro.entity;

import com.fitnesspro.entity.Enums.MembershipStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
@Entity
public class Membership {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(optional = false)
    private Client client;
    @ManyToOne(optional = false)
    private MembershipType membershipType;
    @Column(nullable = false)
    private LocalDate purchaseDate = LocalDate.now();
    @Column(nullable = false)
    private LocalDate activationDate;
    @Column(nullable = false)
    private LocalDate expirationDate;
    private Integer remainingVisits;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private MembershipStatus status = MembershipStatus.ACTIVE;
}
