package com.leavemanagement.leave_management_system.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DocumentDTO {
    private UUID id;
    private String filename;
    private String fileUrl;
    private String fileType;
    private LocalDateTime uploadDate;
    private UUID leaveRequestId;
}