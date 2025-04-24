package com.leavemanagement.leave_management_system.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TeamCalendarDTO {
    private List<LeaveRequestDTO> teamLeaves;
    private List<HolidayDTO> holidays;
    private List<CalendarEventDTO> departmentEvents; // Added field for department events
}