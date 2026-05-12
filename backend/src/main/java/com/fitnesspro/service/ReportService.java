package com.fitnesspro.service;

import com.fitnesspro.dto.Dto.ReportValue;
import com.fitnesspro.entity.Enums.BookingStatus;
import com.fitnesspro.repository.*;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class ReportService {
    private final SaleRepository sales;
    private final VisitRepository visits;
    private final BookingRepository bookings;
    private final ScheduleRepository schedules;

    public ReportService(SaleRepository sales, VisitRepository visits, BookingRepository bookings, ScheduleRepository schedules) {
        this.sales = sales;
        this.visits = visits;
        this.bookings = bookings;
        this.schedules = schedules;
    }

    public List<ReportValue> revenue(LocalDate from, LocalDate to) {
        return List.of(new ReportValue("Доход", sales.revenue(start(from), end(to))));
    }

    public List<ReportValue> attendance(LocalDate from, LocalDate to) {
        return List.of(new ReportValue("Посещения", visits.countByVisitTimeBetween(start(from), end(to))));
    }

    public List<ReportValue> popularTrainingTypes(LocalDate from, LocalDate to) {
        return schedules.findByDateBetweenOrderByDateAscStartTimeAsc(defaultFrom(from), defaultTo(to)).stream()
                .collect(Collectors.groupingBy(s -> s.getTrainingType().getName(),
                        Collectors.summingLong(s -> bookings.countByScheduleAndStatus(s, BookingStatus.ACTIVE)
                                + bookings.countByScheduleAndStatus(s, BookingStatus.ATTENDED))))
                .entrySet().stream()
                .map(e -> new ReportValue(e.getKey(), e.getValue()))
                .toList();
    }

    public List<ReportValue> trainersLoad(LocalDate from, LocalDate to) {
        Map<String, Long> load = schedules.findByDateBetweenOrderByDateAscStartTimeAsc(defaultFrom(from), defaultTo(to)).stream()
                .collect(Collectors.groupingBy(s -> s.getTrainer().getUser().getFullName(), Collectors.counting()));
        return load.entrySet().stream().map(e -> new ReportValue(e.getKey(), e.getValue())).toList();
    }

    private LocalDate defaultFrom(LocalDate from) { return from == null ? LocalDate.now().minusMonths(1) : from; }
    private LocalDate defaultTo(LocalDate to) { return to == null ? LocalDate.now().plusMonths(1) : to; }
    private LocalDateTime start(LocalDate from) { return defaultFrom(from).atStartOfDay(); }
    private LocalDateTime end(LocalDate to) { return defaultTo(to).atTime(LocalTime.MAX); }
}
