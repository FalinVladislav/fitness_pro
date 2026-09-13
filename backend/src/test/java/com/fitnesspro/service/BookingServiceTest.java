package com.fitnesspro.service;

import com.fitnesspro.dto.Dto.BookingRequest;
import com.fitnesspro.entity.Booking;
import com.fitnesspro.entity.Client;
import com.fitnesspro.entity.Enums.BookingStatus;
import com.fitnesspro.entity.Enums.MembershipStatus;
import com.fitnesspro.entity.Enums.Role;
import com.fitnesspro.entity.Enums.ScheduleStatus;
import com.fitnesspro.entity.Membership;
import com.fitnesspro.entity.Schedule;
import com.fitnesspro.entity.Trainer;
import com.fitnesspro.entity.TrainingType;
import com.fitnesspro.entity.User;
import com.fitnesspro.exception.ApiException;
import com.fitnesspro.repository.BookingRepository;
import com.fitnesspro.repository.ClientRepository;
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
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BookingServiceTest {
    private static final long SCHEDULE_ID = 10L;

    @Mock private BookingRepository bookings;
    @Mock private ClientRepository clients;
    @Mock private ScheduleService scheduleService;
    @Mock private MembershipService membershipService;
    @Mock private NotificationService notifications;
    @Mock private Mapper mapper;

    private BookingService service;
    private User currentUser;
    private Client client;

    @BeforeEach
    void setUp() {
        service = new BookingService(bookings, clients, scheduleService, membershipService, notifications, mapper, 120);
        currentUser = user(1L, Role.CLIENT, "client@example.com");
        client = new Client();
        client.setId(5L);
        client.setUser(currentUser);
        when(clients.findByUserEmail(currentUser.getEmail())).thenReturn(Optional.of(client));
    }

    @Test
    void createRejectsClientWithoutActiveMembership() {
        Schedule schedule = upcomingSchedule(ScheduleStatus.PLANNED);
        when(scheduleService.schedule(SCHEDULE_ID)).thenReturn(schedule);
        doThrow(ApiException.badRequest("У клиента нет активного абонемента"))
                .when(membershipService).activeFor(client);

        assertThatThrownBy(() -> service.create(new BookingRequest(SCHEDULE_ID, null), currentUser))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("активного абонемента");

        verify(bookings, never()).save(any());
    }

    @Test
    void createRejectsCancelledTraining() {
        when(scheduleService.schedule(SCHEDULE_ID)).thenReturn(upcomingSchedule(ScheduleStatus.CANCELLED));

        assertThatThrownBy(() -> service.create(new BookingRequest(SCHEDULE_ID, null), currentUser))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("запланированную");

        verifyNoInteractions(membershipService);
    }

    @Test
    void createRejectsTrainingThatHasAlreadyStarted() {
        Schedule schedule = upcomingSchedule(ScheduleStatus.PLANNED);
        schedule.setDate(LocalDate.now());
        schedule.setStartTime(LocalTime.now().minusMinutes(1));
        when(scheduleService.schedule(SCHEDULE_ID)).thenReturn(schedule);

        assertThatThrownBy(() -> service.create(new BookingRequest(SCHEDULE_ID, null), currentUser))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("начавшуюся");

        verifyNoInteractions(membershipService);
    }

    @Test
    void createRejectsDuplicateActiveBooking() {
        Schedule schedule = upcomingSchedule(ScheduleStatus.PLANNED);
        Booking booking = booking(schedule, BookingStatus.ACTIVE);
        when(scheduleService.schedule(SCHEDULE_ID)).thenReturn(schedule);
        when(membershipService.activeFor(client)).thenReturn(activeMembership());
        when(bookings.findByClientAndSchedule(client, schedule)).thenReturn(Optional.of(booking));

        assertThatThrownBy(() -> service.create(new BookingRequest(SCHEDULE_ID, null), currentUser))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("уже записан");
    }

    @Test
    void createRestoresOnlyCancelledBooking() {
        Schedule schedule = upcomingSchedule(ScheduleStatus.PLANNED);
        Booking booking = booking(schedule, BookingStatus.CANCELLED);
        when(scheduleService.schedule(SCHEDULE_ID)).thenReturn(schedule);
        when(membershipService.activeFor(client)).thenReturn(activeMembership());
        when(bookings.findByClientAndSchedule(client, schedule)).thenReturn(Optional.of(booking));
        when(bookings.countByScheduleAndStatus(schedule, BookingStatus.ACTIVE)).thenReturn(0L);

        service.create(new BookingRequest(SCHEDULE_ID, null), currentUser);

        assertThat(booking.getStatus()).isEqualTo(BookingStatus.ACTIVE);
        verify(bookings, never()).save(any());
    }

    @Test
    void createDoesNotRestoreNoShowBooking() {
        Schedule schedule = upcomingSchedule(ScheduleStatus.PLANNED);
        Booking booking = booking(schedule, BookingStatus.NO_SHOW);
        when(scheduleService.schedule(SCHEDULE_ID)).thenReturn(schedule);
        when(membershipService.activeFor(client)).thenReturn(activeMembership());
        when(bookings.findByClientAndSchedule(client, schedule)).thenReturn(Optional.of(booking));
        when(bookings.countByScheduleAndStatus(schedule, BookingStatus.ACTIVE)).thenReturn(0L);

        assertThatThrownBy(() -> service.create(new BookingRequest(SCHEDULE_ID, null), currentUser))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("не может быть восстановлена");
    }

    private Schedule upcomingSchedule(ScheduleStatus status) {
        User trainerUser = user(2L, Role.TRAINER, "trainer@example.com");
        Trainer trainer = new Trainer();
        trainer.setUser(trainerUser);
        TrainingType type = new TrainingType();
        type.setName("Йога");
        Schedule schedule = new Schedule();
        schedule.setId(SCHEDULE_ID);
        schedule.setStatus(status);
        schedule.setDate(LocalDate.now().plusDays(1));
        schedule.setStartTime(LocalTime.of(12, 0));
        schedule.setEndTime(LocalTime.of(13, 0));
        schedule.setParticipantLimit(10);
        schedule.setTrainer(trainer);
        schedule.setTrainingType(type);
        return schedule;
    }

    private Booking booking(Schedule schedule, BookingStatus status) {
        Booking booking = new Booking();
        booking.setClient(client);
        booking.setSchedule(schedule);
        booking.setStatus(status);
        return booking;
    }

    private Membership activeMembership() {
        Membership membership = new Membership();
        membership.setStatus(MembershipStatus.ACTIVE);
        return membership;
    }

    private User user(long id, Role role, String email) {
        User user = new User();
        user.setId(id);
        user.setRole(role);
        user.setEmail(email);
        user.setFullName(email);
        return user;
    }
}
