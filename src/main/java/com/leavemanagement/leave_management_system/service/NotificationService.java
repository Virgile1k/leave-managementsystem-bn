package com.leavemanagement.leave_management_system.service;

import com.leavemanagement.leave_management_system.model.LeaveRequest;
import com.leavemanagement.leave_management_system.model.Notification;
import com.leavemanagement.leave_management_system.model.User;
import com.leavemanagement.leave_management_system.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationService {
    private final NotificationRepository notificationRepository;
    private final EmailService emailService;
    private final UserService userService;

    @Value("${app.frontend.url}")
    private String frontendUrl;

    @Transactional
    public void sendLeaveRequestNotifications(LeaveRequest leaveRequest) {
        // Create in-app notification for the manager
        User manager = leaveRequest.getUser().getManager();
        if (manager != null) {
            createLeaveRequestNotification(manager, leaveRequest);

            // Send email notification to manager
            String message = String.format("%s has requested leave from %s to %s. Please login to approve/reject.",
                    leaveRequest.getUser().getFullName(),
                    leaveRequest.getStartDate(),
                    leaveRequest.getEndDate());

            String approveUrl = frontendUrl + "/leave/approve/" + leaveRequest.getId();
            String rejectUrl = frontendUrl + "/leave/reject/" + leaveRequest.getId();

            String htmlContent = emailService.generateLeaveRequestHtml(
                    manager.getFullName(),
                    "New Leave Request",
                    message,
                    true,
                    approveUrl,
                    rejectUrl
            );

            emailService.sendHtmlMessage(manager.getEmail(), "New Leave Request", htmlContent);
        }

        // Create notification for HR admins
        List<User> hrAdmins = userService.getHRAdmins();
        for (User admin : hrAdmins) {
            createLeaveRequestNotification(admin, leaveRequest);

            // Send email notification to HR admin
            String message = String.format("%s has requested leave from %s to %s. Please review.",
                    leaveRequest.getUser().getFullName(),
                    leaveRequest.getStartDate(),
                    leaveRequest.getEndDate());

            String approveUrl = frontendUrl + "/leave/approve/" + leaveRequest.getId();
            String rejectUrl = frontendUrl + "/leave/reject/" + leaveRequest.getId();

            String htmlContent = emailService.generateLeaveRequestHtml(
                    admin.getFullName(),
                    "New Leave Request",
                    message,
                    true,
                    approveUrl,
                    rejectUrl
            );

            emailService.sendHtmlMessage(admin.getEmail(), "New Leave Request", htmlContent);
        }
    }

    private void createLeaveRequestNotification(User recipient, LeaveRequest leaveRequest) {
        Notification notification = Notification.builder()
                .userId(recipient)
                .title("New Leave Request")
                .content(String.format("%s has requested leave from %s to %s",
                        leaveRequest.getUser().getFullName(),
                        leaveRequest.getStartDate(),
                        leaveRequest.getEndDate()))
                .isRead(false)
                .requestId(leaveRequest)
                .sentAt(LocalDateTime.now())
                .build();

        notificationRepository.save(notification);
    }

    @Transactional
    public void sendLeaveStatusUpdateNotifications(LeaveRequest leaveRequest) {
        // Create in-app notification for the employee
        Notification employeeNotification = Notification.builder()
                .userId(leaveRequest.getUser())
                .title("Leave Request Status Updated")
                .content(String.format("Your leave request from %s to %s has been %s",
                        leaveRequest.getStartDate(),
                        leaveRequest.getEndDate(),
                        leaveRequest.getStatus().getName()))
                .isRead(false)
                .requestId(leaveRequest)
                .sentAt(LocalDateTime.now())
                .build();

        notificationRepository.save(employeeNotification);

        // Send email notification to employee
        String message = String.format("Your leave request from %s to %s has been %s. %s",
                leaveRequest.getStartDate(),
                leaveRequest.getEndDate(),
                leaveRequest.getStatus().getName(),
                leaveRequest.getComments() != null ? "Comments: " + leaveRequest.getComments() : "");

        String htmlContent = emailService.generateLeaveStatusHtml(
                leaveRequest.getUser().getFullName(),
                "Leave Request Status Updated",
                message
        );

        emailService.sendHtmlMessage(
                leaveRequest.getUser().getEmail(),
                "Leave Request Status Updated",
                htmlContent
        );
    }

    @Transactional
    public void sendUpcomingLeaveReminders() {
        // TODO: Implementation for scheduled task to remind employees of upcoming leave
        // Implementation would involve finding leaves that start in X days and sending notifications
    }
}