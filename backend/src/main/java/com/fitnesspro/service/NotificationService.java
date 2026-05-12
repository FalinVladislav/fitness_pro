package com.fitnesspro.service;

import com.fitnesspro.dto.Dto.NotificationDto;
import com.fitnesspro.entity.Enums.NotificationType;
import com.fitnesspro.entity.Notification;
import com.fitnesspro.entity.User;
import com.fitnesspro.exception.ApiException;
import com.fitnesspro.repository.NotificationRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class NotificationService {
    private final NotificationRepository notifications;
    private final Mapper mapper;

    public NotificationService(NotificationRepository notifications, Mapper mapper) {
        this.notifications = notifications;
        this.mapper = mapper;
    }

    @Transactional
    public void notify(User user, String title, String message, NotificationType type) {
        Notification n = new Notification();
        n.setUser(user);
        n.setTitle(title);
        n.setMessage(message);
        n.setType(type);
        notifications.save(n);
    }

    public List<NotificationDto> byUser(User user) {
        return notifications.findByUserOrderByCreatedAtDesc(user).stream().map(mapper::notification).toList();
    }

    @Transactional
    public void read(Long id, User user) {
        Notification n = notifications.findById(id).orElseThrow(() -> ApiException.notFound("Уведомление не найдено"));
        if (!n.getUser().getId().equals(user.getId())) {
            throw ApiException.forbidden("Нельзя читать чужое уведомление");
        }
        n.setReadStatus(true);
    }
}
