package com.leavemanagement.leave_management_system.repository;

import com.leavemanagement.leave_management_system.model.Document;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface DocumentRepository extends JpaRepository<Document, UUID> {
    List<Document> findByUserId(UUID userId);
    List<Document> findByLeaveRequestId(UUID leaveRequestId);
}