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
public class LeaveBalanceAdjustmentDTO {
    private UUID userId;
    private UUID leaveTypeId;
    private Integer year;
    private BigDecimal adjustmentDays;
    private String reason;
}
