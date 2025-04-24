package com.leavemanagement.leave_management_system.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LeaveStatisticsDTO {
    private String name;                              // Name of the entity (department, employee, etc.)
    private String type;                              // Type of the entity (DEPARTMENT, EMPLOYEE, LEAVE_TYPE, COMPANY)
    private LocalDate startDate;                      // Start date of the report period
    private LocalDate endDate;                        // End date of the report period
    private long totalRequests;                       // Total number of leave requests
    private long approvedRequests;                    // Number of approved leave requests
    private BigDecimal totalLeaveDays;                // Total number of leave days taken
    private BigDecimal averageLeaveDuration;          // Average duration of leave requests
    private List<LeaveTypeSummaryDTO> leaveTypeBreakdown;  // Breakdown by leave type
    private Map<String, BigDecimal> monthlyDistribution;   // Monthly distribution of leave days
}