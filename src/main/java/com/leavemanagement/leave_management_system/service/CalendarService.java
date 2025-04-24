package com.leavemanagement.leave_management_system.service;

import com.leavemanagement.leave_management_system.dto.CalendarEventDTO;
import com.leavemanagement.leave_management_system.dto.HolidayDTO;
import com.leavemanagement.leave_management_system.dto.LeaveRequestDTO;
import com.leavemanagement.leave_management_system.dto.TeamCalendarDTO;
import com.leavemanagement.leave_management_system.exceptions.ResourceNotFoundException;
import com.leavemanagement.leave_management_system.exceptions.UnauthorizedException;
import com.leavemanagement.leave_management_system.model.CalendarEvent;
import com.leavemanagement.leave_management_system.model.Holiday;
import com.leavemanagement.leave_management_system.model.LeaveRequest;
import com.leavemanagement.leave_management_system.model.User;
import com.leavemanagement.leave_management_system.repository.CalendarEventRepository;
import com.leavemanagement.leave_management_system.repository.HolidayRepository;
import com.leavemanagement.leave_management_system.repository.LeaveRequestRepository;
import com.microsoft.graph.models.Event;
import com.microsoft.graph.models.ItemBody;
import com.microsoft.graph.models.BodyType;
import com.microsoft.graph.models.DateTimeTimeZone;
import com.microsoft.graph.requests.GraphServiceClient;
import lombok.extern.slf4j.Slf4j;
import okhttp3.Request;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Slf4j
public class CalendarService {
    private final CalendarEventRepository calendarEventRepository;
    private final HolidayRepository holidayRepository;
    private final LeaveRequestRepository leaveRequestRepository;

    // Optional GraphServiceClient - may be null if Outlook integration is disabled
    private final GraphServiceClient<Request> graphClient;

    @Value("${outlook.calendar.enabled:false}")
    private boolean outlookCalendarEnabled;

    @Value("${azure.default-user-email:}")
    private String defaultUserEmail;

    @Value("${azure.retry-attempts:3}")
    private int retryAttempts;

    @Value("${azure.retry-delay-ms:2000}")
    private int retryDelayMs;

    @Autowired
    public CalendarService(
            CalendarEventRepository calendarEventRepository,
            HolidayRepository holidayRepository,
            LeaveRequestRepository leaveRequestRepository,
            @Autowired(required = false) GraphServiceClient<Request> graphClient) {
        this.calendarEventRepository = calendarEventRepository;
        this.holidayRepository = holidayRepository;
        this.leaveRequestRepository = leaveRequestRepository;
        this.graphClient = graphClient;
    }

    /**
     * Create a calendar event in the internal system
     * If Outlook integration is enabled and configured, it will also create an event in Outlook
     */
    @Transactional
    public CalendarEventDTO createCalendarEvent(CalendarEventDTO eventDTO, UUID departmentId, UUID userId) {
        // Convert ZonedDateTime to LocalDateTime if necessary
        LocalDateTime startTime = eventDTO.getStartTime() instanceof ZonedDateTime ?
                ((ZonedDateTime) eventDTO.getStartTime()).toLocalDateTime() :
                (LocalDateTime) eventDTO.getStartTime();

        LocalDateTime endTime = eventDTO.getEndTime() instanceof ZonedDateTime ?
                ((ZonedDateTime) eventDTO.getEndTime()).toLocalDateTime() :
                (LocalDateTime) eventDTO.getEndTime();

        // Ensure referenceId is never null by providing a default UUID if needed
        UUID referenceId = eventDTO.getReferenceId() != null ?
                eventDTO.getReferenceId() : UUID.randomUUID();

        CalendarEvent calendarEvent = CalendarEvent.builder()
                .title(eventDTO.getTitle())
                .description(eventDTO.getDescription())
                .startTime(startTime)
                .endTime(endTime)
                .eventType(eventDTO.getEventType())
                .referenceId(referenceId)
                .departmentId(departmentId)  // Add department ID
                .createdBy(userId)           // Add user ID who created the event
                .build();

        CalendarEvent savedEvent = calendarEventRepository.save(calendarEvent);

        // Only sync with Outlook if integration is enabled and properly configured
        if (isOutlookIntegrationActive()) {
            try {
                String outlookEventId = createOutlookEvent(eventDTO);
                savedEvent.setOutlookEventId(outlookEventId);
                savedEvent = calendarEventRepository.save(savedEvent);
                log.debug("Event created in Outlook with ID: {}", outlookEventId);
            } catch (Exception e) {
                log.warn("Failed to create event in Outlook, but internal calendar event was created successfully", e);
                // Do not fail the entire operation if Outlook sync fails
            }
        }

        return convertToCalendarEventDTO(savedEvent);
    }

    @Transactional
    public CalendarEventDTO updateCalendarEvent(CalendarEventDTO eventDTO, UUID departmentId, UUID userId) {
        CalendarEvent calendarEvent = calendarEventRepository.findById(eventDTO.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Calendar event not found"));

        // Security check: Only allow updates if the event belongs to the user's department
        // or if the user has special permissions (could be added as additional logic)
        if (calendarEvent.getDepartmentId() != null && !calendarEvent.getDepartmentId().equals(departmentId)) {
            throw new UnauthorizedException("You don't have permission to update this calendar event");
        }

        calendarEvent.setTitle(eventDTO.getTitle());
        calendarEvent.setDescription(eventDTO.getDescription());

        // Convert ZonedDateTime to LocalDateTime if necessary
        LocalDateTime startTime = eventDTO.getStartTime() instanceof ZonedDateTime ?
                ((ZonedDateTime) eventDTO.getStartTime()).toLocalDateTime() :
                (LocalDateTime) eventDTO.getStartTime();

        LocalDateTime endTime = eventDTO.getEndTime() instanceof ZonedDateTime ?
                ((ZonedDateTime) eventDTO.getEndTime()).toLocalDateTime() :
                (LocalDateTime) eventDTO.getEndTime();

        calendarEvent.setStartTime(startTime);
        calendarEvent.setEndTime(endTime);

        // Ensure referenceId is never null when updating
        if (eventDTO.getReferenceId() != null) {
            calendarEvent.setReferenceId(eventDTO.getReferenceId());
        }

        // Do not update departmentId or createdBy as that should remain constant

        CalendarEvent updatedEvent = calendarEventRepository.save(calendarEvent);

        // Only sync with Outlook if integration is enabled, properly configured,
        // and the event already has an Outlook ID
        if (isOutlookIntegrationActive() && calendarEvent.getOutlookEventId() != null) {
            try {
                updateOutlookEvent(calendarEvent.getOutlookEventId(), eventDTO);
                log.debug("Event updated in Outlook with ID: {}", calendarEvent.getOutlookEventId());
            } catch (Exception e) {
                log.warn("Failed to update event in Outlook, but internal calendar event was updated successfully", e);
                // Do not fail the entire operation if Outlook sync fails
            }
        }

        return convertToCalendarEventDTO(updatedEvent);
    }

    @Transactional
    public void deleteCalendarEvent(UUID eventId, UUID departmentId, UUID userId) {
        CalendarEvent calendarEvent = calendarEventRepository.findById(eventId)
                .orElseThrow(() -> new ResourceNotFoundException("Calendar event not found"));

        // Security check: Only allow deletion if the event belongs to the user's department
        // or if the user has special permissions (could be added as additional logic)
        if (calendarEvent.getDepartmentId() != null && !calendarEvent.getDepartmentId().equals(departmentId)) {
            throw new UnauthorizedException("You don't have permission to delete this calendar event");
        }

        // If Outlook integration is enabled and the event has an Outlook ID, delete it from Outlook
        if (isOutlookIntegrationActive() && calendarEvent.getOutlookEventId() != null) {
            try {
                deleteOutlookEvent(calendarEvent.getOutlookEventId());
                log.debug("Event deleted from Outlook with ID: {}", calendarEvent.getOutlookEventId());
            } catch (Exception e) {
                log.warn("Failed to delete event from Outlook, but internal calendar event will be deleted", e);
                // Do not fail the entire operation if Outlook sync fails
            }
        }

        calendarEventRepository.delete(calendarEvent);
    }

    public List<CalendarEvent> getCalendarEventsByReferenceId(UUID referenceId) {
        return calendarEventRepository.findByReferenceId(referenceId);
    }

    public List<CalendarEventDTO> getCalendarEventsByDepartment(LocalDateTime startTime, LocalDateTime endTime, UUID departmentId) {
        return calendarEventRepository.findByDateRangeAndDepartment(startTime, endTime, departmentId).stream()
                .map(this::convertToCalendarEventDTO)
                .collect(Collectors.toList());
    }

    /**
     * Create a new holiday
     * @param holidayDTO The holiday data to be created
     * @return The created holiday data
     */
    @Transactional
    public HolidayDTO createHoliday(HolidayDTO holidayDTO) {
        Holiday holiday = Holiday.builder()
                .name(holidayDTO.getName())
                .date(holidayDTO.getDate())
                .isRecurring(holidayDTO.getIsRecurring())
                .countryId(holidayDTO.getCountryId())
                .build();

        Holiday savedHoliday = holidayRepository.save(holiday);
        return convertToHolidayDTO(savedHoliday);
    }

    @Deprecated
    public List<CalendarEventDTO> getCalendarEvents(LocalDateTime startTime, LocalDateTime endTime) {
        // This method is kept for backward compatibility but should not be used
        // as it doesn't filter by department
        log.warn("Using deprecated getCalendarEvents method without department filtering");
        return calendarEventRepository.findByDateRange(startTime, endTime).stream()
                .map(this::convertToCalendarEventDTO)
                .collect(Collectors.toList());
    }

    public List<HolidayDTO> getHolidays(LocalDate startDate, LocalDate endDate) {
        return holidayRepository.findHolidaysBetweenDates(startDate, endDate).stream()
                .map(this::convertToHolidayDTO)
                .collect(Collectors.toList());
    }

    public TeamCalendarDTO getTeamCalendar(UUID departmentId, LocalDate startDate, LocalDate endDate) {
        // Get all approved leave requests for the department
        List<LeaveRequest> teamLeaves = leaveRequestRepository.findByDepartmentAndDateRange(
                departmentId, startDate, endDate);

        // Get all holidays for the date range
        List<Holiday> holidays = holidayRepository.findHolidaysBetweenDates(startDate, endDate);

        // Get department-specific calendar events
        LocalDateTime startDateTime = startDate.atStartOfDay();
        LocalDateTime endDateTime = endDate.atTime(23, 59, 59);
        List<CalendarEventDTO> departmentEvents = getCalendarEventsByDepartment(startDateTime, endDateTime, departmentId);

        return TeamCalendarDTO.builder()
                .teamLeaves(teamLeaves.stream()
                        .filter(leave -> "APPROVED".equals(leave.getStatus().getName()))
                        .map(leave -> {
                            User user = leave.getUser();

                            return LeaveRequestDTO.builder()
                                    .id(leave.getId())
                                    .userId(user.getId())
                                    .userName(user.getFullName())  // Add user name here
                                    .email(user.getEmail())        // Optionally add email
                                    .leaveTypeId(leave.getLeaveType().getId())
                                    .leaveTypeName(leave.getLeaveType().getName())
                                    .startDate(leave.getStartDate())
                                    .endDate(leave.getEndDate())
                                    .status(leave.getStatus().getName())
                                    .reason(leave.getReason())
                                    .fullDay(leave.getFullDay())
                                    .leaveDuration(leave.getLeaveDuration())
                                    .build();
                        })
                        .collect(Collectors.toList()))
                .holidays(holidays.stream()
                        .map(this::convertToHolidayDTO)
                        .collect(Collectors.toList()))
                .departmentEvents(departmentEvents) // Add department calendar events
                .build();
    }
    /**
     * Synchronizes calendar events with Outlook.
     * This is a placeholder method that could be implemented in the future.
     * Currently not implemented as there's no immediate need for full sync.
     */
    public void syncOutlookCalendar() {
        if (!isOutlookIntegrationActive()) {
            throw new IllegalStateException("Outlook calendar integration is not configured or disabled");
        }

        log.info("Outlook calendar sync initiated");
        // Implementation would go here if needed in the future
        log.info("Outlook calendar sync completed");
    }

    // Helper method to check if Outlook integration is properly configured and enabled
    private boolean isOutlookIntegrationActive() {
        return outlookCalendarEnabled && graphClient != null && defaultUserEmail != null && !defaultUserEmail.isEmpty();
    }

    // Outlook integration methods - only called if integration is active
    private String createOutlookEvent(CalendarEventDTO eventDTO) {
        if (!isOutlookIntegrationActive()) {
            throw new IllegalStateException("Outlook calendar is not available");
        }

        Event event = new Event();
        event.subject = eventDTO.getTitle();

        ItemBody body = new ItemBody();
        body.contentType = BodyType.TEXT;
        body.content = eventDTO.getDescription();
        event.body = body;

        DateTimeTimeZone start = new DateTimeTimeZone();
        // Handle both LocalDateTime and ZonedDateTime
        if (eventDTO.getStartTime() instanceof ZonedDateTime) {
            ZonedDateTime zonedDateTime = (ZonedDateTime) eventDTO.getStartTime();
            start.dateTime = zonedDateTime.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME);
            start.timeZone = zonedDateTime.getZone().getId();
        } else {
            LocalDateTime localDateTime = (LocalDateTime) eventDTO.getStartTime();
            start.dateTime = localDateTime.atZone(ZoneId.systemDefault())
                    .format(DateTimeFormatter.ISO_OFFSET_DATE_TIME);
            start.timeZone = ZoneId.systemDefault().getId();
        }
        event.start = start;

        DateTimeTimeZone end = new DateTimeTimeZone();
        // Handle both LocalDateTime and ZonedDateTime
        if (eventDTO.getEndTime() instanceof ZonedDateTime) {
            ZonedDateTime zonedDateTime = (ZonedDateTime) eventDTO.getEndTime();
            end.dateTime = zonedDateTime.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME);
            end.timeZone = zonedDateTime.getZone().getId();
        } else {
            LocalDateTime localDateTime = (LocalDateTime) eventDTO.getEndTime();
            end.dateTime = localDateTime.atZone(ZoneId.systemDefault())
                    .format(DateTimeFormatter.ISO_OFFSET_DATE_TIME);
            end.timeZone = ZoneId.systemDefault().getId();
        }
        event.end = end;

        // Using client credentials flow with application permissions
        Event createdEvent = graphClient.users(defaultUserEmail).events()
                .buildRequest()
                .post(event);

        return createdEvent.id;
    }

    private void updateOutlookEvent(String outlookEventId, CalendarEventDTO eventDTO) {
        if (!isOutlookIntegrationActive()) {
            throw new IllegalStateException("Outlook calendar is not available");
        }

        Event event = new Event();
        event.subject = eventDTO.getTitle();

        ItemBody body = new ItemBody();
        body.contentType = BodyType.TEXT;
        body.content = eventDTO.getDescription();
        event.body = body;

        DateTimeTimeZone start = new DateTimeTimeZone();
        // Handle both LocalDateTime and ZonedDateTime
        if (eventDTO.getStartTime() instanceof ZonedDateTime) {
            ZonedDateTime zonedDateTime = (ZonedDateTime) eventDTO.getStartTime();
            start.dateTime = zonedDateTime.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME);
            start.timeZone = zonedDateTime.getZone().getId();
        } else {
            LocalDateTime localDateTime = (LocalDateTime) eventDTO.getStartTime();
            start.dateTime = localDateTime.atZone(ZoneId.systemDefault())
                    .format(DateTimeFormatter.ISO_OFFSET_DATE_TIME);
            start.timeZone = ZoneId.systemDefault().getId();
        }
        event.start = start;

        DateTimeTimeZone end = new DateTimeTimeZone();
        // Handle both LocalDateTime and ZonedDateTime
        if (eventDTO.getEndTime() instanceof ZonedDateTime) {
            ZonedDateTime zonedDateTime = (ZonedDateTime) eventDTO.getEndTime();
            end.dateTime = zonedDateTime.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME);
            end.timeZone = zonedDateTime.getZone().getId();
        } else {
            LocalDateTime localDateTime = (LocalDateTime) eventDTO.getEndTime();
            end.dateTime = localDateTime.atZone(ZoneId.systemDefault())
                    .format(DateTimeFormatter.ISO_OFFSET_DATE_TIME);
            end.timeZone = ZoneId.systemDefault().getId();
        }
        event.end = end;

        // Using client credentials flow with application permissions
        graphClient.users(defaultUserEmail).events(outlookEventId)
                .buildRequest()
                .patch(event);
    }

    private void deleteOutlookEvent(String outlookEventId) {
        if (!isOutlookIntegrationActive()) {
            throw new IllegalStateException("Outlook calendar is not available");
        }

        // Using client credentials flow with application permissions
        graphClient.users(defaultUserEmail).events(outlookEventId)
                .buildRequest()
                .delete();
    }

    private CalendarEventDTO convertToCalendarEventDTO(CalendarEvent calendarEvent) {
        return CalendarEventDTO.builder()
                .id(calendarEvent.getId())
                .title(calendarEvent.getTitle())
                .description(calendarEvent.getDescription())
                .startTime(calendarEvent.getStartTime())
                .endTime(calendarEvent.getEndTime())
                .eventType(calendarEvent.getEventType())
                .referenceId(calendarEvent.getReferenceId())
                .outlookEventId(calendarEvent.getOutlookEventId())
                .build();
    }

    private HolidayDTO convertToHolidayDTO(Holiday holiday) {
        return HolidayDTO.builder()
                .id(holiday.getId())
                .name(holiday.getName())
                .date(holiday.getDate())
                .isRecurring(holiday.getIsRecurring())
                .countryId(holiday.getCountryId())
                .build();
    }
}