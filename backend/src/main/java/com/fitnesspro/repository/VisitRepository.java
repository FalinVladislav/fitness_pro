package com.fitnesspro.repository;

import com.fitnesspro.entity.Client;
import com.fitnesspro.entity.Visit;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface VisitRepository extends JpaRepository<Visit, Long> {
    List<Visit> findByClientOrderByVisitTimeDesc(Client client);
    long countByVisitTimeBetween(LocalDateTime from, LocalDateTime to);
}
