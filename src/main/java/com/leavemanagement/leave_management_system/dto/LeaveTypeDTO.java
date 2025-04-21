package com.leavemanagement.leave_management_system.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LeaveTypeDTO {
    private UUID id;
    private String name;
    private String description;
    private BigDecimal accrualRate;
    private Boolean requiresDoc;
    private Integer maxDays;
    private Boolean isActive;
}
