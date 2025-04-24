package com.leavemanagement.leave_management_system.repository;

import com.leavemanagement.leave_management_system.dto.LeaveRequestDTO;
import com.leavemanagement.leave_management_system.model.Notification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, UUID> {

    List<Notification> findByUserIdAndIsReadFalse(UUID userId);

    // Add this method to count unread notifications for a user
    long countByUserIdAndIsReadFalse(UUID userId);

    List<Notification> findByUserIdOrderByCreatedAtDesc(UUID userId);

    @Query("SELECT new com.leavemanagement.leave_management_system.dto.LeaveRequestDTO(lr.id, lr.user.id, lr.leaveType.id, " +
            "lr.leaveType.name, lr.startDate, lr.endDate, lr.status.name, lr.reason, lr.fullDay, lr.comments, null, null) " +
            "FROM LeaveRequest lr " +
            "WHERE lr.startDate = :date " +
            "AND lr.status.name = 'APPROVED'")
    List<LeaveRequestDTO> findUpcomingLeaves(@Param("date") LocalDate date);

    @Query("SELECT new com.leavemanagement.leave_management_system.dto.LeaveRequestDTO(lr.id, lr.user.id, lr.leaveType.id, " +
            "lr.leaveType.name, lr.startDate, lr.endDate, lr.status.name, lr.reason, lr.fullDay, lr.comments, " +
            "lr.user.department.id, null) " +
            "FROM LeaveRequest lr " +
            "WHERE lr.status.name = 'PENDING' " +
            "AND lr.createdAt < :cutoffDate")
    List<LeaveRequestDTO> findPendingRequests(@Param("cutoffDate") LocalDateTime cutoffDate);
}