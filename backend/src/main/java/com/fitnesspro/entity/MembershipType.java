package com.fitnesspro.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@Entity
public class MembershipType {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false)
    private String name;
    @Column(nullable = false)
    private Integer durationDays;
    private Integer visitCount;
    @Column(nullable = false)
    private BigDecimal price;
    @Column(length = 1000)
    private String description;
    @Column(nullable = false)
    private boolean active = true;
    @Column(nullable = false)
    private boolean freezeAllowed = false;
    @Column(nullable = false)
    private Integer maxFreezeDays = 0;
}
