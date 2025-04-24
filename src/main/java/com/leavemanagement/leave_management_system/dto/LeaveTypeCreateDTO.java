package com.leavemanagement.leave_management_system.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LeaveTypeCreateDTO {
    private String name;
    private String description;
    private BigDecimal accrualRate;
    private Boolean requiresDoc;
    private Integer maxDays;
}