package com.leavemanagement.leave_management_system.controller;

import com.leavemanagement.leave_management_system.model.Notification;
import com.leavemanagement.leave_management_system.service.NotificationService;
import com.leavemanagement.leave_management_system.util.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;
    private final SecurityUtils securityUtils;

    // Get all notifications for the current user
    @GetMapping
    @PreAuthorize("hasAnyRole('STAFF', 'MANAGER', 'ADMIN')")
    public ResponseEntity<List<Notification>> getAllNotifications() {
        UUID userId = securityUtils.getCurrentUserId();
        return ResponseEntity.ok(notificationService.getAllNotifications(userId));
    }

    // Get unread notifications for the current user
    @GetMapping("/unread")
    @PreAuthorize("hasAnyRole('STAFF', 'MANAGER', 'ADMIN')")
    public ResponseEntity<List<Notification>> getUnreadNotifications() {
        UUID userId = securityUtils.getCurrentUserId();
        return ResponseEntity.ok(notificationService.getUnreadNotifications(userId));
    }

    // Mark a notification as read
    @PutMapping("/{notificationId}/read")
    @PreAuthorize("hasAnyRole('STAFF', 'MANAGER', 'ADMIN')")
    public ResponseEntity<Notification> markAsRead(@PathVariable UUID notificationId) {
        return ResponseEntity.ok(notificationService.markAsRead(notificationId));
    }

    // Mark all notifications as read
    @PutMapping("/read-all")
    @PreAuthorize("hasAnyRole('STAFF', 'MANAGER', 'ADMIN')")
    public ResponseEntity<Void> markAllAsRead() {
        UUID userId = securityUtils.getCurrentUserId();
        notificationService.markAllAsRead(userId);
        return ResponseEntity.ok().build();
    }
}