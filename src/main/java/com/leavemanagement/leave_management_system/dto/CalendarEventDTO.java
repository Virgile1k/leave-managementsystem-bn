package com.leavemanagement.leave_management_system.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.leavemanagement.leave_management_system.config.DateTimeDeserializer;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CalendarEventDTO {
    private UUID id;
    private String title;
    private String description;

    @JsonDeserialize(using = DateTimeDeserializer.class)
    private Object startTime; // Can be either LocalDateTime or ZonedDateTime

    @JsonDeserialize(using = DateTimeDeserializer.class)
    private Object endTime; // Can be either LocalDateTime or ZonedDateTime

    private String eventType;
    private UUID referenceId;
    private String outlookEventId;

    // Added fields for department information
    private UUID departmentId;
    private String departmentName;
}