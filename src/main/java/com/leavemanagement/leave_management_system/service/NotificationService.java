package com.leavemanagement.leave_management_system.service;

import com.leavemanagement.leave_management_system.dto.LeaveRequestDTO;
import com.leavemanagement.leave_management_system.enums.NotificationType;
import com.leavemanagement.leave_management_system.model.Notification;
import com.leavemanagement.leave_management_system.model.NotificationTemplate;
import com.leavemanagement.leave_management_system.model.User;
import com.leavemanagement.leave_management_system.repository.NotificationRepository;
import com.leavemanagement.leave_management_system.repository.NotificationTemplateRepository;
import com.leavemanagement.leave_management_system.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final NotificationTemplateRepository notificationTemplateRepository; // Added
    private final UserRepository userRepository;
    private final EmailService emailService;

    @Value("${app.frontend.url}")
    private String frontendUrl;

    /**
     * Create and save a notification in the database
     */
    public Notification createNotification(UUID userId, String title, String message, NotificationType type, UUID referenceId) {
        LocalDateTime now = LocalDateTime.now();

        // Fetch the NotificationTemplate based on NotificationType
        NotificationTemplate template = notificationTemplateRepository.findByEventType(type.name())
                .orElseThrow(() -> new IllegalStateException("No template found for event type: " + type.name()));

        Notification notification = Notification.builder()
                .userId(userId)
                .title(title)
                .message(message)
                .content(message)
                .type(type)
                .requestId(referenceId)
                .isRead(false)
                .createdAt(now)
                .sentAt(now)
                .updatedAt(now)
                .template(template) // Set the template
                .build();

        return notificationRepository.save(notification);
    }

    /**
     * Send a notification for a leave request that was submitted
     */
    public void notifyLeaveRequestSubmitted(LeaveRequestDTO leaveRequest, List<User> managers) {
        User employee = userRepository.findById(leaveRequest.getUserId())
                .orElseThrow(() -> new RuntimeException("User not found"));

        // Notify employee
        String employeeMessage = "Your leave request has been submitted and is pending approval.";
        createNotification(
                employee.getId(),
                "Leave Request Submitted",
                employeeMessage,
                NotificationType.LEAVE_SUBMITTED,
                leaveRequest.getId()
        );

        // Send email to employee
        String employeeEmail = employee.getEmail();
        String employeeHtml = emailService.generateLeaveStatusHtml(
                employee.getFullName(),
                "Leave Request Submitted",
                employeeMessage
        );
        emailService.sendHtmlMessage(employeeEmail, "Leave Request Submitted", employeeHtml);

        // Notify all managers
        for (User manager : managers) {
            String managerMessage = employee.getFullName() + " has submitted a leave request that requires your approval.";
            createNotification(
                    manager.getId(),
                    "New Leave Request Pending Approval",
                    managerMessage,
                    NotificationType.LEAVE_APPROVAL_PENDING,
                    leaveRequest.getId()
            );

            // Send email to managers with approval links
            String approveUrl = frontendUrl + "/leaves/approve/" + leaveRequest.getId();
            String rejectUrl = frontendUrl + "/leaves/reject/" + leaveRequest.getId();

            String managerHtml = emailService.generateLeaveRequestHtml(
                    manager.getFullName(),
                    "Leave Request Pending Approval",
                    managerMessage,
                    true,
                    approveUrl,
                    rejectUrl
            );

            emailService.sendHtmlMessage(manager.getEmail(), "Leave Request Pending Approval", managerHtml);
        }
    }

    /**
     * Send a notification for a leave request that was updated (approved or rejected)
     */
    public void notifyLeaveRequestUpdated(LeaveRequestDTO leaveRequest, String status) {
        User employee = userRepository.findById(leaveRequest.getUserId())
                .orElseThrow(() -> new RuntimeException("User not found"));

        String title = "Leave Request " + status;
        String message = "Your leave request has been " + status.toLowerCase() + ".";
        if (leaveRequest.getComments() != null && !leaveRequest.getComments().isEmpty()) {
            message += " Comments: " + leaveRequest.getComments();
        }

        // Create in-app notification
        createNotification(
                employee.getId(),
                title,
                message,
                "APPROVED".equals(status) ? NotificationType.LEAVE_APPROVED : NotificationType.LEAVE_REJECTED,
                leaveRequest.getId()
        );

        // Send email notification
        String html = emailService.generateLeaveStatusHtml(
                employee.getFullName(),
                title,
                message
        );

        emailService.sendHtmlMessage(employee.getEmail(), title, html);
    }

    /**
     * Get all unread notifications for a user
     */
    public List<Notification> getUnreadNotifications(UUID userId) {
        return notificationRepository.findByUserIdAndIsReadFalse(userId);
    }

    /**
     * Get all notifications for a user
     */
    public List<Notification> getAllNotifications(UUID userId) {
        return notificationRepository.findByUserIdOrderByCreatedAtDesc(userId);
    }

    /**
     * Mark a notification as read
     */
    public Notification markAsRead(UUID notificationId) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new RuntimeException("Notification not found"));

        notification.setIsRead(true);
        notification.setReadAt(LocalDateTime.now());

        return notificationRepository.save(notification);
    }

    /**
     * Mark all notifications for a user as read
     */
    public void markAllAsRead(UUID userId) {
        List<Notification> unreadNotifications = notificationRepository.findByUserIdAndIsReadFalse(userId);

        for (Notification notification : unreadNotifications) {
            notification.setIsRead(true);
            notification.setReadAt(LocalDateTime.now());
        }

        notificationRepository.saveAll(unreadNotifications);
    }


    /**
     * Daily job to send reminders for upcoming leaves (one day before)
     */
    @Scheduled(cron = "0 0 8 * * ?") // Run every day at 8:00 AM
    public void sendUpcomingLeaveReminders() {
        log.info("Running scheduled job to send upcoming leave reminders");

        LocalDate tomorrow = LocalDate.now().plusDays(1);

        // Find all approved leave requests starting tomorrow
        List<LeaveRequestDTO> upcomingLeaves = notificationRepository.findUpcomingLeaves(tomorrow);

        for (LeaveRequestDTO leave : upcomingLeaves) {
            User employee = userRepository.findById(leave.getUserId())
                    .orElseThrow(() -> new RuntimeException("User not found"));

            // Notify employee
            String message = "Your " + leave.getLeaveTypeName() + " leave starts tomorrow.";
            createNotification(
                    employee.getId(),
                    "Upcoming Leave Reminder",
                    message,
                    NotificationType.LEAVE_REMINDER,
                    leave.getId()
            );

            // Send email to employee
            String html = emailService.generateLeaveStatusHtml(
                    employee.getFullName(),
                    "Upcoming Leave Reminder",
                    message
            );

            emailService.sendHtmlMessage(employee.getEmail(), "Upcoming Leave Reminder", html);
        }
    }

    /**
     * Daily job to send reminders for pending approvals (if pending for more than 2 days)
     */
    @Scheduled(cron = "0 0 9 * * ?") // Run every day at 9:00 AM
    public void sendPendingApprovalReminders() {
        log.info("Running scheduled job to send pending approval reminders");

        LocalDateTime twoDaysAgo = LocalDateTime.now().minusDays(2);

        // Find all leave requests that have been pending for more than 2 days
        List<LeaveRequestDTO> pendingRequests = notificationRepository.findPendingRequests(twoDaysAgo);

        for (LeaveRequestDTO leave : pendingRequests) {
            // Get all managers who need to approve
            List<User> managers = userRepository.findManagersByDepartmentId(leave.getDepartmentId());

            User employee = userRepository.findById(leave.getUserId())
                    .orElseThrow(() -> new RuntimeException("User not found"));

            for (User manager : managers) {
                // Notify managers
                String message = "A leave request from " + employee.getFullName() + " has been pending for more than 2 days.";
                createNotification(
                        manager.getId(),
                        "Pending Approval Reminder",
                        message,
                        NotificationType.APPROVAL_REMINDER,
                        leave.getId()
                );

                // Send email to managers with approval links
                String approveUrl = frontendUrl + "/leaves/approve/" + leave.getId();
                String rejectUrl = frontendUrl + "/leaves/reject/" + leave.getId();

                String html = emailService.generateLeaveRequestHtml(
                        manager.getFullName(),
                        "Pending Approval Reminder",
                        message,
                        true,
                        approveUrl,
                        rejectUrl
                );

                emailService.sendHtmlMessage(manager.getEmail(), "Pending Approval Reminder", html);
            }
        }
    }
}