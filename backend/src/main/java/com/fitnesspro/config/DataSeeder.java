package com.fitnesspro.config;

import com.fitnesspro.entity.*;
import com.fitnesspro.entity.Enums.*;
import com.fitnesspro.repository.*;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;

@Configuration
public class DataSeeder {
    @Bean
    CommandLineRunner seed(UserRepository users, ClientRepository clients, TrainerRepository trainers,
                           AdministratorRepository admins, MembershipTypeRepository membershipTypes,
                           MembershipRepository memberships, HallRepository halls, TrainingTypeRepository trainingTypes,
                           ScheduleRepository schedules, SaleRepository sales, PasswordEncoder encoder) {
        return args -> {
            if (users.existsByEmail("admin@example.com")) {
                return;
            }

            User adminUser = user("Администратор Фитнес-Про", "admin@example.com", "+79990000001", "admin123", Role.ADMIN, encoder);
            users.save(adminUser);
            Administrator admin = new Administrator();
            admin.setUser(adminUser);
            admin.setPosition("Старший администратор");
            admins.save(admin);

            users.save(user("Руководитель Фитнес-Про", "manager@example.com", "+79990000002", "manager123", Role.MANAGER, encoder));

            User trainerUser = user("Иван Петров", "trainer@example.com", "+79990000003", "trainer123", Role.TRAINER, encoder);
            users.save(trainerUser);
            Trainer trainer = new Trainer();
            trainer.setUser(trainerUser);
            trainer.setSpecialization("Силовой тренинг, функциональные тренировки");
            trainer.setDescription("Тренер с опытом 8 лет, ведет групповые и персональные занятия.");
            trainers.save(trainer);

            User trainerUser2 = user("Анна Соколова", "anna.trainer@example.com", "+79990000004", "trainer123", Role.TRAINER, encoder);
            users.save(trainerUser2);
            Trainer trainer2 = new Trainer();
            trainer2.setUser(trainerUser2);
            trainer2.setSpecialization("Йога, пилатес");
            trainer2.setDescription("Специалист по мягкому фитнесу и восстановлению.");
            trainers.save(trainer2);

            User clientUser = user("Мария Иванова", "client@example.com", "+79990000005", "client123", Role.CLIENT, encoder);
            users.save(clientUser);
            Client client = new Client();
            client.setUser(clientUser);
            client.setRfidCard("FP-0001");
            client.setBirthDate(LocalDate.of(2001, 5, 14));
            clients.save(client);

            MembershipType monthly = membershipType("Месяц безлимит", 30, null, "Безлимитное посещение клуба на 30 дней", new BigDecimal("3500"));
            MembershipType visits = membershipType("8 тренировок", 45, 8, "Лимитированный абонемент на групповые тренировки", new BigDecimal("2800"));
            membershipTypes.save(monthly);
            membershipTypes.save(visits);

            Hall main = hall("Большой зал", 25, "Зал групповых программ");
            Hall yoga = hall("Зал йоги", 14, "Тихий зал с ковриками и мягким светом");
            halls.save(main);
            halls.save(yoga);

            TrainingType functional = trainingType("Functional PRO", 60, "Интенсивная функциональная тренировка", "#f97316");
            TrainingType yogaType = trainingType("Morning Yoga", 75, "Йога для гибкости и восстановления", "#22c55e");
            trainingTypes.save(functional);
            trainingTypes.save(yogaType);

            Membership membership = new Membership();
            membership.setClient(client);
            membership.setMembershipType(monthly);
            membership.setActivationDate(LocalDate.now().minusDays(3));
            membership.setExpirationDate(LocalDate.now().plusDays(27));
            membership.setRemainingVisits(null);
            membership.setStatus(MembershipStatus.ACTIVE);
            memberships.save(membership);

            Sale sale = new Sale();
            sale.setClient(client);
            sale.setMembership(membership);
            sale.setAmount(monthly.getPrice());
            sale.setPaymentMethod(PaymentMethod.CARD);
            sales.save(sale);

            schedules.save(schedule(functional, trainer, main, LocalDate.now().plusDays(1), LocalTime.of(18, 0), 20));
            schedules.save(schedule(yogaType, trainer2, yoga, LocalDate.now().plusDays(2), LocalTime.of(9, 0), 12));
        };
    }

    private User user(String name, String email, String phone, String password, Role role, PasswordEncoder encoder) {
        User user = new User();
        user.setFullName(name);
        user.setEmail(email);
        user.setPhone(phone);
        user.setPasswordHash(encoder.encode(password));
        user.setRole(role);
        return user;
    }

    private MembershipType membershipType(String name, int days, Integer visits, String description, BigDecimal price) {
        MembershipType type = new MembershipType();
        type.setName(name);
        type.setDurationDays(days);
        type.setVisitCount(visits);
        type.setDescription(description);
        type.setPrice(price);
        type.setActive(true);
        return type;
    }

    private Hall hall(String name, int capacity, String description) {
        Hall hall = new Hall();
        hall.setName(name);
        hall.setCapacity(capacity);
        hall.setDescription(description);
        return hall;
    }

    private TrainingType trainingType(String name, int minutes, String description, String color) {
        TrainingType type = new TrainingType();
        type.setName(name);
        type.setDurationMinutes(minutes);
        type.setDescription(description);
        type.setColor(color);
        return type;
    }

    private Schedule schedule(TrainingType type, Trainer trainer, Hall hall, LocalDate date, LocalTime start, int limit) {
        Schedule schedule = new Schedule();
        schedule.setTrainingType(type);
        schedule.setTrainer(trainer);
        schedule.setHall(hall);
        schedule.setDate(date);
        schedule.setStartTime(start);
        schedule.setEndTime(start.plusMinutes(type.getDurationMinutes()));
        schedule.setParticipantLimit(limit);
        return schedule;
    }
}
