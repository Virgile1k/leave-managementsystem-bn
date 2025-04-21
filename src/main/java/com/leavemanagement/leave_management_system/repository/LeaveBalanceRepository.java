package com.leavemanagement.leave_management_system.repository;

import com.leavemanagement.leave_management_system.model.LeaveBalance;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface LeaveBalanceRepository extends JpaRepository<LeaveBalance, UUID> {
    @Query("SELECT lb FROM LeaveBalance lb WHERE lb.user.id = :userId AND lb.leaveType.id = :leaveTypeId AND lb.year = :year")
    Optional<LeaveBalance> findByUserIdAndLeaveTypeIdAndYear(UUID userId, UUID leaveTypeId, Integer year);

    List<LeaveBalance> findByUserIdAndYear(UUID userId, Integer year);
}