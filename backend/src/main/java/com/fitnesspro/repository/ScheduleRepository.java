package com.fitnesspro.repository;

import com.fitnesspro.entity.Schedule;
import com.fitnesspro.entity.Trainer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

public interface ScheduleRepository extends JpaRepository<Schedule, Long> {
    List<Schedule> findByTrainerOrderByDateAscStartTimeAsc(Trainer trainer);
    List<Schedule> findByDateBetweenOrderByDateAscStartTimeAsc(LocalDate from, LocalDate to);

    @Query("""
        select count(s) > 0 from Schedule s
        where s.trainer.id = :trainerId
          and s.date = :date and s.id <> :ignoreId
          and s.startTime < :endTime and s.endTime > :startTime
        """)
    boolean trainerHasConflict(Long trainerId, LocalDate date, LocalTime startTime, LocalTime endTime, Long ignoreId);

    @Query("""
        select count(s) > 0 from Schedule s
        where s.hall.id = :hallId
          and s.date = :date and s.id <> :ignoreId
          and s.startTime < :endTime and s.endTime > :startTime
        """)
    boolean hallHasConflict(Long hallId, LocalDate date, LocalTime startTime, LocalTime endTime, Long ignoreId);
}
