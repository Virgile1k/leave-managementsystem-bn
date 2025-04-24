package com.leavemanagement.leave_management_system.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
public class DocumentDTO {
    private UUID id;
    private String filename;
    private String fileUrl;
    private String fileType;
    private LocalDateTime createdAt;
    private UUID userId;
    private UUID leaveRequestId;
}