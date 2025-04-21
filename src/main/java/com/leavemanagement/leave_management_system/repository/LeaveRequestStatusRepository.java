package com.leavemanagement.leave_management_system.repository;

import com.leavemanagement.leave_management_system.model.LeaveRequestStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface LeaveRequestStatusRepository extends JpaRepository<LeaveRequestStatus, UUID> {
    Optional<LeaveRequestStatus> findByName(String name);
}