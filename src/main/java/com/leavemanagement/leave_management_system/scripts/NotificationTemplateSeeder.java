package com.leavemanagement.leave_management_system.scripts;

//import com.example.notifications.model.NotificationTemplate;
//import com.example.notifications.repository.NotificationTemplateRepository;
import com.leavemanagement.leave_management_system.model.NotificationTemplate;
import com.leavemanagement.leave_management_system.repository.NotificationTemplateRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.UUID;

@Component
public class NotificationTemplateSeeder implements CommandLineRunner {

    private final NotificationTemplateRepository notificationTemplateRepository;

    @Autowired
    public NotificationTemplateSeeder(NotificationTemplateRepository notificationTemplateRepository) {
        this.notificationTemplateRepository = notificationTemplateRepository;
    }

    @Override
    public void run(String... args) {
        // First check if data already exists to avoid duplicate seeding
        if (notificationTemplateRepository.count() == 0) {
            seedNotificationTemplates();
        }
    }

    private void seedNotificationTemplates() {
        LocalDateTime now = LocalDateTime.now();

        NotificationTemplate[] templates = {
                createTemplate(
                        UUID.fromString("7e7921b8-d12d-46bd-a4c4-a9c7bc881873"),
                        "Your leave request has been submitted.",
                        "LEAVE_SUBMITTED",
                        "Leave Submitted",
                        "Leave Request Submitted",
                        now
                ),
                createTemplate(
                        UUID.fromString("d00dddb8-4d2c-4f62-b1da-0c15b58ca379"),
                        "A leave request requires your approval.",
                        "LEAVE_APPROVAL_PENDING",
                        "Leave Approval Pending",
                        "Leave Request Pending Approval",
                        now
                ),
                createTemplate(
                        UUID.fromString("44d7689c-5a3d-42bf-8cd2-7e83793ce518"),
                        "Your leave request has been approved.",
                        "LEAVE_APPROVED",
                        "Leave Approved",
                        "Leave Request Approved",
                        now
                ),
                createTemplate(
                        UUID.fromString("f6771197-5363-4e9e-be13-943dba020239"),
                        "Your leave request has been rejected.",
                        "LEAVE_REJECTED",
                        "Leave Rejected",
                        "Leave Request Rejected",
                        now
                ),
                createTemplate(
                        UUID.fromString("9ec1a56f-ee9e-4ab8-93fb-a1a7762260c8"),
                        "Your leave starts soon.",
                        "LEAVE_REMINDER",
                        "Leave Reminder",
                        "Upcoming Leave Reminder",
                        now
                ),
                createTemplate(
                        UUID.fromString("cc98c0e9-9646-49e6-8d32-e43f6bc8d214"),
                        "A leave request is pending your approval.",
                        "APPROVAL_REMINDER",
                        "Approval Reminder",
                        "Pending Approval Reminder",
                        now
                )
        };

        notificationTemplateRepository.saveAll(Arrays.asList(templates));
        System.out.println("Notification templates seeded successfully");
    }

    private NotificationTemplate createTemplate(UUID id, String bodyTemplate, String eventType,
                                                String name, String subject, LocalDateTime timestamp) {
        NotificationTemplate template = new NotificationTemplate();
        template.setId(id);
        template.setBodyTemplate(bodyTemplate);
        template.setEventType(eventType);
        template.setName(name);
        template.setSubject(subject);
        template.setCreatedAt(timestamp);
        template.setUpdatedAt(timestamp);
        return template;
    }
}