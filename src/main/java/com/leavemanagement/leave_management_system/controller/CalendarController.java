package com.leavemanagement.leave_management_system.controller;

import com.leavemanagement.leave_management_system.dto.CalendarEventDTO;
import com.leavemanagement.leave_management_system.dto.HolidayDTO;
import com.leavemanagement.leave_management_system.dto.TeamCalendarDTO;
import com.leavemanagement.leave_management_system.dto.UserResponseDto;
import com.leavemanagement.leave_management_system.service.CalendarService;
import com.leavemanagement.leave_management_system.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/calendar")
@RequiredArgsConstructor
@Slf4j
public class CalendarController {
    private final CalendarService calendarService;
    private final UserService userService;

    @GetMapping("/events")
    public ResponseEntity<List<CalendarEventDTO>> getCalendarEvents(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startTime,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endTime,
            @RequestParam(required = false) UUID departmentId) {

        // Get the authenticated user's department if departmentId is not provided
        if (departmentId == null) {
            // Get current authenticated user
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            String userEmail = auth.getName();
            UserResponseDto user = userService.getUserByEmail(userEmail);
            departmentId = user.getDepartmentId();
        }

        return ResponseEntity.ok(calendarService.getCalendarEventsByDepartment(startTime, endTime, departmentId));
    }

    @GetMapping("/events/reference/{referenceId}")
    public ResponseEntity<List<CalendarEventDTO>> getCalendarEventsByReferenceId(
            @PathVariable UUID referenceId) {
        return ResponseEntity.ok(
                calendarService.getCalendarEventsByReferenceId(referenceId).stream()
                        .map(event -> CalendarEventDTO.builder()
                                .id(event.getId())
                                .title(event.getTitle())
                                .description(event.getDescription())
                                .startTime(event.getStartTime())
                                .endTime(event.getEndTime())
                                .eventType(event.getEventType())
                                .referenceId(event.getReferenceId())
                                .outlookEventId(event.getOutlookEventId())
                                .build())
                        .toList()
        );
    }

    @PostMapping("/events")
    public ResponseEntity<CalendarEventDTO> createCalendarEvent(@RequestBody CalendarEventDTO eventDTO) {
        // Get current authenticated user
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String userEmail = auth.getName();
        UserResponseDto user = userService.getUserByEmail(userEmail);

        // Handle both LocalDateTime and ZonedDateTime formats
        validateDateTimes(eventDTO);

        // Set default referenceId if null
        if (eventDTO.getReferenceId() == null) {
            eventDTO.setReferenceId(UUID.randomUUID());
        }

        return new ResponseEntity<>(
                calendarService.createCalendarEvent(eventDTO, user.getDepartmentId(), user.getId()),
                HttpStatus.CREATED
        );
    }

    @PutMapping("/events/{eventId}")
    public ResponseEntity<CalendarEventDTO> updateCalendarEvent(
            @PathVariable UUID eventId,
            @RequestBody CalendarEventDTO eventDTO) {
        // Get current authenticated user
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String userEmail = auth.getName();
        UserResponseDto user = userService.getUserByEmail(userEmail);

        // Handle both LocalDateTime and ZonedDateTime formats
        validateDateTimes(eventDTO);

        eventDTO.setId(eventId);
        return ResponseEntity.ok(calendarService.updateCalendarEvent(eventDTO, user.getDepartmentId(), user.getId()));
    }

    @PostMapping("/holidays")
    public ResponseEntity<HolidayDTO> createHoliday(@RequestBody HolidayDTO holidayDTO) {
        // Get current authenticated user for authorization check
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String userEmail = auth.getName();
        UserResponseDto user = userService.getUserByEmail(userEmail);
        HolidayDTO createdHoliday = calendarService.createHoliday(holidayDTO);

        return new ResponseEntity<>(createdHoliday, HttpStatus.CREATED);
    }

    @DeleteMapping("/events/{eventId}")
    public ResponseEntity<Void> deleteCalendarEvent(@PathVariable UUID eventId) {
        // Get current authenticated user for authorization check
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String userEmail = auth.getName();
        UserResponseDto user = userService.getUserByEmail(userEmail);

        calendarService.deleteCalendarEvent(eventId, user.getDepartmentId(), user.getId());
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/holidays")
    public ResponseEntity<List<HolidayDTO>> getHolidays(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        return ResponseEntity.ok(calendarService.getHolidays(startDate, endDate));
    }

    @GetMapping("/team/{departmentId}")
    public ResponseEntity<TeamCalendarDTO> getTeamCalendar(
            @PathVariable UUID departmentId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        return ResponseEntity.ok(calendarService.getTeamCalendar(departmentId, startDate, endDate));
    }

    @GetMapping("/team")
    public ResponseEntity<TeamCalendarDTO> getCurrentUserTeamCalendar(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        // Get current authenticated user's department
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String userEmail = auth.getName();
        UserResponseDto user = userService.getUserByEmail(userEmail);

        return ResponseEntity.ok(calendarService.getTeamCalendar(user.getDepartmentId(), startDate, endDate));
    }

    /**
     * Helper method to validate and handle different datetime formats
     */
    private void validateDateTimes(CalendarEventDTO eventDTO) {
        // Check if start time is after end time
        LocalDateTime startTime = getLocalDateTime(eventDTO.getStartTime());
        LocalDateTime endTime = getLocalDateTime(eventDTO.getEndTime());

        if (startTime.isAfter(endTime)) {
            throw new IllegalArgumentException("Start time cannot be after end time");
        }
    }

    /**
     * Convert either LocalDateTime or ZonedDateTime to LocalDateTime
     */
    private LocalDateTime getLocalDateTime(Object dateTime) {
        if (dateTime instanceof ZonedDateTime) {
            return ((ZonedDateTime) dateTime).toLocalDateTime();
        } else if (dateTime instanceof LocalDateTime) {
            return (LocalDateTime) dateTime;
        } else {
            throw new IllegalArgumentException("Invalid datetime format provided");
        }
    }
}