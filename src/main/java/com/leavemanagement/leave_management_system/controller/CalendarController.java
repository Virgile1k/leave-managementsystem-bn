package com.leavemanagement.leave_management_system.controller;


import com.leavemanagement.leave_management_system.dto.CalendarEventDTO;
import com.leavemanagement.leave_management_system.dto.DateRangeDTO;
import com.leavemanagement.leave_management_system.dto.HolidayDTO;
import com.leavemanagement.leave_management_system.dto.TeamCalendarDTO;
import com.leavemanagement.leave_management_system.service.CalendarService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/calendar")
@RequiredArgsConstructor
public class CalendarController {
    private final CalendarService calendarService;

    @GetMapping("/events")
    public ResponseEntity<List<CalendarEventDTO>> getCalendarEvents(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startTime,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endTime) {
        return ResponseEntity.ok(calendarService.getCalendarEvents(startTime, endTime));
    }

    @PostMapping("/events")
    public ResponseEntity<CalendarEventDTO> createCalendarEvent(@RequestBody CalendarEventDTO eventDTO) {
        return new ResponseEntity<>(calendarService.createCalendarEvent(eventDTO), HttpStatus.CREATED);
    }

    @PutMapping("/events/{eventId}")
    public ResponseEntity<CalendarEventDTO> updateCalendarEvent(
            @PathVariable UUID eventId,
            @RequestBody CalendarEventDTO eventDTO) {
        eventDTO.setId(eventId);
        return ResponseEntity.ok(calendarService.updateCalendarEvent(eventDTO));
    }

    @DeleteMapping("/events/{eventId}")
    public ResponseEntity<Void> deleteCalendarEvent(@PathVariable UUID eventId) {
        calendarService.deleteCalendarEvent(eventId);
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

    @PostMapping("/sync/outlook")
    public ResponseEntity<Void> syncOutlookCalendar() {
        calendarService.syncOutlookCalendar();
        return ResponseEntity.ok().build();
    }
}
