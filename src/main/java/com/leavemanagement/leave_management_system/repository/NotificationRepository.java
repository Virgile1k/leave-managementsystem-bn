package com.leavemanagement.leave_management_system.repository;

import com.leavemanagement.leave_management_system.model.Notification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, UUID> {
    List<Notification> findByUserIdIdOrderBySentAtDesc(UUID userId);
    List<Notification> findByUserIdIdAndIsReadOrderBySentAtDesc(UUID userId, Boolean isRead);
    long countByUserIdIdAndIsRead(UUID userId, Boolean isRead);
}