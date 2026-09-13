package com.fitnesspro.service;

import com.fitnesspro.dto.Dto.ScheduleRequest;
import com.fitnesspro.entity.Enums.Role;
import com.fitnesspro.entity.Enums.ScheduleStatus;
import com.fitnesspro.entity.Schedule;
import com.fitnesspro.entity.User;
import com.fitnesspro.exception.ApiException;
import com.fitnesspro.repository.BookingRepository;
import com.fitnesspro.repository.HallRepository;
import com.fitnesspro.repository.ScheduleRepository;
import com.fitnesspro.repository.TrainerRepository;
import com.fitnesspro.repository.TrainingTypeRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ScheduleServiceTest {
    @Mock private ScheduleRepository schedules;
    @Mock private TrainingTypeRepository trainingTypes;
    @Mock private TrainerRepository trainers;
    @Mock private HallRepository halls;
    @Mock private BookingRepository bookings;
    @Mock private NotificationService notifications;
    @Mock private Mapper mapper;

    @Test
    void saveRejectsCompletedTraining() {
        Schedule schedule = new Schedule();
        schedule.setStatus(ScheduleStatus.COMPLETED);
        when(schedules.findById(1L)).thenReturn(Optional.of(schedule));

        assertThatThrownBy(() -> service().save(1L, request(), admin()))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("Нельзя изменять");
    }

    @Test
    void completeRejectsTrainingBeforeItsEndTime() {
        Schedule schedule = new Schedule();
        schedule.setStatus(ScheduleStatus.PLANNED);
        schedule.setDate(LocalDate.now());
        schedule.setEndTime(LocalTime.now().plusMinutes(10));
        when(schedules.findById(1L)).thenReturn(Optional.of(schedule));

        assertThatThrownBy(() -> service().complete(1L, admin()))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("времени его окончания");
    }

    private ScheduleService service() {
        return new ScheduleService(schedules, trainingTypes, trainers, halls, bookings, notifications, mapper);
    }

    private ScheduleRequest request() {
        return new ScheduleRequest(1L, 1L, 1L, LocalDate.now().plusDays(1), LocalTime.NOON, null, 10, null);
    }

    private User admin() {
        User user = new User();
        user.setRole(Role.ADMIN);
        return user;
    }
}
