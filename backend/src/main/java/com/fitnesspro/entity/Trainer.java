package com.fitnesspro.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
public class Trainer {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(optional = false)
    private User user;

    private String specialization;

    @Column(nullable = false)
    private Integer yearsOfExperience = 0;

    @Column(length = 1000)
    private String description;
}
