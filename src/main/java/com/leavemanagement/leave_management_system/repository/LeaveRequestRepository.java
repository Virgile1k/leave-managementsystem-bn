package com.leavemanagement.leave_management_system.repository;


import com.leavemanagement.leave_management_system.model.LeaveRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Repository
public interface LeaveRequestRepository extends JpaRepository<LeaveRequest, UUID> {
    List<LeaveRequest> findByUserId(UUID userId);

    @Query("SELECT lr FROM LeaveRequest lr WHERE lr.user.id = :userId AND lr.status.name = :status")
    List<LeaveRequest> findByUserIdAndStatus(UUID userId, String status);

    @Query("SELECT lr FROM LeaveRequest lr WHERE lr.status.name = :status")
    List<LeaveRequest> findByStatus(String status);

    @Query("SELECT lr FROM LeaveRequest lr WHERE lr.user.department.id = :departmentId AND " +
            "((lr.startDate BETWEEN :startDate AND :endDate) OR " +
            "(lr.endDate BETWEEN :startDate AND :endDate) OR " +
            "(lr.startDate <= :startDate AND lr.endDate >= :endDate))")
    List<LeaveRequest> findByDepartmentAndDateRange(UUID departmentId, LocalDate startDate, LocalDate endDate);




}