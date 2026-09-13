package com.fitnesspro.service;

import com.fitnesspro.dto.Dto.CheckInRequest;
import com.fitnesspro.dto.Dto.VisitDto;
import com.fitnesspro.entity.Booking;
import com.fitnesspro.entity.Client;
import com.fitnesspro.entity.Enums.BookingStatus;
import com.fitnesspro.entity.Enums.Role;
import com.fitnesspro.entity.Enums.ScheduleStatus;
import com.fitnesspro.entity.Enums.VisitType;
import com.fitnesspro.entity.Membership;
import com.fitnesspro.entity.Schedule;
import com.fitnesspro.entity.Trainer;
import com.fitnesspro.entity.User;
import com.fitnesspro.exception.ApiException;
import com.fitnesspro.repository.BookingRepository;
import com.fitnesspro.repository.ClientRepository;
import com.fitnesspro.repository.ScheduleRepository;
import com.fitnesspro.repository.VisitRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class VisitServiceTest {
    private static final long CLIENT_ID = 10L;
    private static final long SCHEDULE_ID = 20L;

    @Mock private VisitRepository visits;
    @Mock private ClientRepository clients;
    @Mock private ScheduleRepository schedules;
    @Mock private BookingRepository bookings;
    @Mock private MembershipService membershipService;
    @Mock private Mapper mapper;

    private VisitService service;
    private Client client;
    private User trainer;
    private Schedule schedule;

    @BeforeEach
    void setUp() {
        service = new VisitService(visits, clients, schedules, bookings, membershipService, mapper);
        client = new Client();
        client.setId(CLIENT_ID);
        client.setUser(user(1L, Role.CLIENT));
        trainer = user(2L, Role.TRAINER);
        schedule = activeSchedule();
        when(clients.findById(CLIENT_ID)).thenReturn(Optional.of(client));
    }

    @Test
    void checkInRequiresActiveBooking() {
        when(schedules.findById(SCHEDULE_ID)).thenReturn(Optional.of(schedule));
        when(bookings.findByClientAndSchedule(client, schedule)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.checkIn(new CheckInRequest(CLIENT_ID, SCHEDULE_ID, VisitType.GROUP_TRAINING), trainer))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("не записан");

        verifyNoInteractions(membershipService);
    }

    @Test
    void checkInRejectsDuplicateVisitForTraining() {
        when(schedules.findById(SCHEDULE_ID)).thenReturn(Optional.of(schedule));
        when(bookings.findByClientAndSchedule(client, schedule)).thenReturn(Optional.of(activeBooking()));
        when(visits.existsByClientAndSchedule(client, schedule)).thenReturn(true);

        assertThatThrownBy(() -> service.checkIn(new CheckInRequest(CLIENT_ID, SCHEDULE_ID, VisitType.GROUP_TRAINING), trainer))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("уже отмечено");

        verifyNoInteractions(membershipService);
    }

    @Test
    void checkInDebitsMembershipAndMarksBookingAttended() {
        Booking booking = activeBooking();
        when(schedules.findById(SCHEDULE_ID)).thenReturn(Optional.of(schedule));
        Membership membership = new Membership();
        when(bookings.findByClientAndSchedule(client, schedule)).thenReturn(Optional.of(booking));
        when(visits.existsByClientAndSchedule(client, schedule)).thenReturn(false);
        when(membershipService.activeFor(client)).thenReturn(membership);
        when(mapper.visit(any())).thenReturn(new VisitDto(1L, CLIENT_ID, "Клиент", null, SCHEDULE_ID, null, VisitType.GROUP_TRAINING));

        service.checkIn(new CheckInRequest(CLIENT_ID, SCHEDULE_ID, VisitType.GROUP_TRAINING), trainer);

        verify(membershipService).chargeVisit(membership);
        verify(visits).save(any());
        assertThat(booking.getStatus()).isEqualTo(BookingStatus.ATTENDED);
    }

    @Test
    void gymCheckInRejectsSecondVisitOnSameDay() {
        User admin = user(3L, Role.ADMIN);
        when(visits.existsByClientAndScheduleIsNullAndVisitTimeBetween(eq(client), any(), any())).thenReturn(true);

        assertThatThrownBy(() -> service.checkIn(new CheckInRequest(CLIENT_ID, null, VisitType.GYM), admin))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("уже отмечено сегодня");

        verifyNoInteractions(membershipService);
    }

    private Schedule activeSchedule() {
        Trainer trainerProfile = new Trainer();
        trainerProfile.setUser(trainer);
        Schedule value = new Schedule();
        value.setId(SCHEDULE_ID);
        value.setStatus(ScheduleStatus.PLANNED);
        value.setDate(LocalDate.now());
        value.setStartTime(LocalTime.now().minusMinutes(10));
        value.setEndTime(LocalTime.now().plusMinutes(10));
        value.setTrainer(trainerProfile);
        return value;
    }

    private Booking activeBooking() {
        Booking booking = new Booking();
        booking.setClient(client);
        booking.setSchedule(schedule);
        booking.setStatus(BookingStatus.ACTIVE);
        return booking;
    }

    private User user(long id, Role role) {
        User user = new User();
        user.setId(id);
        user.setRole(role);
        user.setEmail(role.name().toLowerCase() + "@example.com");
        user.setFullName(role.name());
        return user;
    }
}
