package com.leavemanagement.leave_management_system.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LeaveReportDTO {
    private UUID id;
    private String employeeName;
    private String employeeEmail;
    private String departmentName;
    private String leaveTypeName;
    private LocalDate startDate;
    private LocalDate endDate;
    private String status;
    private BigDecimal duration;
    private String reason;
    private String comments;
}