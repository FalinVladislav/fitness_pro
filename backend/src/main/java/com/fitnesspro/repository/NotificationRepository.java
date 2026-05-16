package com.fitnesspro.repository;

import com.fitnesspro.entity.Notification;
import com.fitnesspro.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface NotificationRepository extends JpaRepository<Notification, Long> {
    List<Notification> findByUserOrderByCreatedAtDesc(User user);
    boolean existsByUserAndTitleAndMessage(User user, String title, String message);
}
