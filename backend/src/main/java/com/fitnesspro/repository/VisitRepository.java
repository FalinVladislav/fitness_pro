package com.fitnesspro.repository;

import com.fitnesspro.entity.Client;
import com.fitnesspro.entity.Visit;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface VisitRepository extends JpaRepository<Visit, Long> {
    List<Visit> findByClientOrderByVisitTimeDesc(Client client);
    List<Visit> findByScheduleTrainerUserEmailOrderByVisitTimeDesc(String email);
    long countByVisitTimeBetween(LocalDateTime from, LocalDateTime to);
    boolean existsByClientAndSchedule(Client client, com.fitnesspro.entity.Schedule schedule);
    boolean existsByClientAndScheduleIsNullAndVisitTimeBetween(Client client, LocalDateTime from, LocalDateTime to);
}
