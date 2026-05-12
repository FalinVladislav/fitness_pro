package com.fitnesspro.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
@Entity
public class Client {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(optional = false)
    private User user;

    @Column(nullable = false)
    private LocalDate registrationDate = LocalDate.now();

    @Column(unique = true)
    private String rfidCard;

    private LocalDate birthDate;
}
