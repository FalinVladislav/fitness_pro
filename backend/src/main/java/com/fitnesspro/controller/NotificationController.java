package com.fitnesspro.controller;

import com.fitnesspro.dto.Dto.NotificationDto;
import com.fitnesspro.service.AuthService;
import com.fitnesspro.service.NotificationService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/notifications")
public class NotificationController {
    private final NotificationService notifications;
    private final AuthService auth;

    public NotificationController(NotificationService notifications, AuthService auth) {
        this.notifications = notifications;
        this.auth = auth;
    }

    @GetMapping
    public List<NotificationDto> list() {
        return notifications.byUser(auth.currentUser());
    }

    @PostMapping("/{id}/read")
    public void read(@PathVariable Long id) {
        notifications.read(id, auth.currentUser());
    }
}
