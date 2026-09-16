package com.fitnesspro.controller;

import com.fitnesspro.dto.Dto.NotificationDto;
import com.fitnesspro.service.AuthService;
import com.fitnesspro.service.NotificationService;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/notifications")
@Tag(name = "Notifications", description = "Current user's notifications")
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
