package com.leavemanagement.leave_management_system.repository;

import com.leavemanagement.leave_management_system.model.CalendarEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface CalendarEventRepository extends JpaRepository<CalendarEvent, UUID> {
    List<CalendarEvent> findByReferenceId(UUID referenceId);

    Optional<CalendarEvent> findByOutlookEventId(String outlookEventId);

    @Query("SELECT c FROM CalendarEvent c WHERE c.startTime BETWEEN :startTime AND :endTime OR c.endTime BETWEEN :startTime AND :endTime")
    List<CalendarEvent> findByDateRange(LocalDateTime startTime, LocalDateTime endTime);

    @Query("SELECT c FROM CalendarEvent c WHERE (c.startTime BETWEEN :startTime AND :endTime OR c.endTime BETWEEN :startTime AND :endTime) AND (c.departmentId = :departmentId OR c.isGlobal = true)")
    List<CalendarEvent> findByDateRangeAndDepartment(LocalDateTime startTime, LocalDateTime endTime, UUID departmentId);

    @Query("SELECT c FROM CalendarEvent c WHERE c.createdBy = :userId AND (c.startTime BETWEEN :startTime AND :endTime OR c.endTime BETWEEN :startTime AND :endTime)")
    List<CalendarEvent> findByDateRangeAndCreatedBy(LocalDateTime startTime, LocalDateTime endTime, UUID userId);
}