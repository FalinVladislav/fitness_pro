package com.fitnesspro.repository;

import com.fitnesspro.entity.Booking;
import com.fitnesspro.entity.Client;
import com.fitnesspro.entity.Enums.BookingStatus;
import com.fitnesspro.entity.Schedule;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface BookingRepository extends JpaRepository<Booking, Long> {
    List<Booking> findByClientOrderByBookingDateTimeDesc(Client client);
    List<Booking> findBySchedule(Schedule schedule);
    long countByScheduleAndStatus(Schedule schedule, BookingStatus status);
    Optional<Booking> findByClientAndSchedule(Client client, Schedule schedule);
    List<Booking> findByScheduleAndStatus(Schedule schedule, BookingStatus status);
}
