package com.leavemanagement.leave_management_system.service;

import com.leavemanagement.leave_management_system.dto.CalendarEventDTO;
import com.leavemanagement.leave_management_system.dto.HolidayDTO;
import com.leavemanagement.leave_management_system.dto.LeaveRequestDTO;
import com.leavemanagement.leave_management_system.dto.TeamCalendarDTO;
import com.leavemanagement.leave_management_system.exceptions.ResourceNotFoundException;
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
import com.microsoft.graph.requests.EventCollectionPage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class CalendarService {
    private final CalendarEventRepository calendarEventRepository;
    private final HolidayRepository holidayRepository;
    private final LeaveRequestRepository leaveRequestRepository;
    private final UserService userService;

    @Value("${outlook.calendar.enabled:false}")
    private boolean outlookCalendarEnabled;

    private GraphServiceClient graphClient;

    // This method would be called by a scheduled task or manually to sync events
    @Transactional
    public void syncOutlookCalendar() {
        if (!outlookCalendarEnabled) {
            log.info("Outlook calendar sync is disabled");
            return;
        }

        try {
            // Get events from Outlook within a date range (e.g., next 3 months)
            LocalDateTime now = LocalDateTime.now();
            LocalDateTime endDate = now.plusMonths(3);

            // Fetch events from Outlook
            EventCollectionPage events = fetchOutlookEvents(now, endDate);

            // Process each event from Outlook
            for (Event event : events.getCurrentPage()) {
                // Check if event already exists in our system
                calendarEventRepository.findByOutlookEventId(event.id)
                        .ifPresentOrElse(
                                existingEvent -> updateExistingEventFromOutlook(existingEvent, event),
                                () -> createNewEventFromOutlook(event)
                        );
            }

            // For events in our system that have an Outlook ID, check if they still exist in Outlook
            // If not, either update or remove them
            syncLocalEventsWithOutlook(now, endDate);

        } catch (Exception e) {
            log.error("Error syncing with Outlook calendar", e);
        }
    }

    private EventCollectionPage fetchOutlookEvents(LocalDateTime startDateTime, LocalDateTime endDateTime) {
        if (graphClient == null) {
            initializeGraphClient();
        }

        String startDateTimeString = startDateTime.atZone(ZoneId.systemDefault())
                .format(DateTimeFormatter.ISO_OFFSET_DATE_TIME);
        String endDateTimeString = endDateTime.atZone(ZoneId.systemDefault())
                .format(DateTimeFormatter.ISO_OFFSET_DATE_TIME);

        return graphClient.me().calendar().events()
                .buildRequest()
                .filter("start/dateTime ge '" + startDateTimeString + "' and end/dateTime le '" + endDateTimeString + "'")
                .get();
    }

    private void initializeGraphClient() {
        // Implementation depends on the authentication method used
        // This would typically involve using OAuth credentials or a client certificate

        // Placeholder for actual implementation
        log.info("Initializing Microsoft Graph client");

        // Usually, you would create an authentication provider with the appropriate credentials
        // TokenCredentialAuthProvider authProvider = new TokenCredentialAuthProvider(scopes, credential);
        // graphClient = GraphServiceClient.builder().authenticationProvider(authProvider).buildClient();
    }

    private void updateExistingEventFromOutlook(CalendarEvent existingEvent, Event outlookEvent) {
        existingEvent.setTitle(outlookEvent.subject);
        existingEvent.setDescription(outlookEvent.bodyPreview);

        DateTimeFormatter formatter = DateTimeFormatter.ISO_OFFSET_DATE_TIME;
        existingEvent.setStartTime(LocalDateTime.parse(outlookEvent.start.dateTime, formatter));
        existingEvent.setEndTime(LocalDateTime.parse(outlookEvent.end.dateTime, formatter));

        calendarEventRepository.save(existingEvent);
    }

    private void createNewEventFromOutlook(Event outlookEvent) {
        DateTimeFormatter formatter = DateTimeFormatter.ISO_OFFSET_DATE_TIME;
        LocalDateTime startTime = LocalDateTime.parse(outlookEvent.start.dateTime, formatter);
        LocalDateTime endTime = LocalDateTime.parse(outlookEvent.end.dateTime, formatter);

        CalendarEvent newEvent = CalendarEvent.builder()
                .title(outlookEvent.subject)
                .description(outlookEvent.bodyPreview)
                .startTime(startTime)
                .endTime(endTime)
                .eventType("OUTLOOK")
                .outlookEventId(outlookEvent.id)
                .referenceId(UUID.randomUUID())  // Generate a placeholder reference ID
                .build();

        calendarEventRepository.save(newEvent);
    }

    private void syncLocalEventsWithOutlook(LocalDateTime startDateTime, LocalDateTime endDateTime) {
        List<CalendarEvent> localEvents = calendarEventRepository.findByDateRange(startDateTime, endDateTime);

        for (CalendarEvent localEvent : localEvents) {
            if (localEvent.getOutlookEventId() != null) {
                try {
                    // Check if event still exists in Outlook
                    Event outlookEvent = graphClient.me().events(localEvent.getOutlookEventId())
                            .buildRequest()
                            .get();

                    // Update the local event if needed
                    updateExistingEventFromOutlook(localEvent, outlookEvent);
                } catch (Exception e) {
                    // If event doesn't exist in Outlook anymore, clear the outlook ID
                    log.info("Event no longer exists in Outlook: {}", localEvent.getOutlookEventId());
                    localEvent.setOutlookEventId(null);
                    calendarEventRepository.save(localEvent);
                }
            }
        }
    }

    @Transactional
    public CalendarEventDTO createCalendarEvent(CalendarEventDTO eventDTO) {
        CalendarEvent calendarEvent = CalendarEvent.builder()
                .title(eventDTO.getTitle())
                .description(eventDTO.getDescription())
                .startTime(eventDTO.getStartTime())
                .endTime(eventDTO.getEndTime())
                .eventType(eventDTO.getEventType())
                .referenceId(eventDTO.getReferenceId())
                .build();

        CalendarEvent savedEvent = calendarEventRepository.save(calendarEvent);

        // If Outlook integration is enabled, sync the event to Outlook
        if (outlookCalendarEnabled) {
            try {
                String outlookEventId = createOutlookEvent(eventDTO);
                savedEvent.setOutlookEventId(outlookEventId);
                savedEvent = calendarEventRepository.save(savedEvent);
            } catch (Exception e) {
                log.error("Failed to create event in Outlook", e);
            }
        }

        return convertToCalendarEventDTO(savedEvent);
    }

    @Transactional
    public CalendarEventDTO updateCalendarEvent(CalendarEventDTO eventDTO) {
        CalendarEvent calendarEvent = calendarEventRepository.findById(eventDTO.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Calendar event not found"));

        calendarEvent.setTitle(eventDTO.getTitle());
        calendarEvent.setDescription(eventDTO.getDescription());
        calendarEvent.setStartTime(eventDTO.getStartTime());
        calendarEvent.setEndTime(eventDTO.getEndTime());

        CalendarEvent updatedEvent = calendarEventRepository.save(calendarEvent);

        // If Outlook integration is enabled and the event has an Outlook ID, update it
        if (outlookCalendarEnabled && calendarEvent.getOutlookEventId() != null) {
            try {
                updateOutlookEvent(calendarEvent.getOutlookEventId(), eventDTO);
            } catch (Exception e) {
                log.error("Failed to update event in Outlook", e);
            }
        }

        return convertToCalendarEventDTO(updatedEvent);
    }

    @Transactional
    public void deleteCalendarEvent(UUID eventId) {
        CalendarEvent calendarEvent = calendarEventRepository.findById(eventId)
                .orElseThrow(() -> new ResourceNotFoundException("Calendar event not found"));

        // If Outlook integration is enabled and the event has an Outlook ID, delete it from Outlook
        if (outlookCalendarEnabled && calendarEvent.getOutlookEventId() != null) {
            try {
                deleteOutlookEvent(calendarEvent.getOutlookEventId());
            } catch (Exception e) {
                log.error("Failed to delete event from Outlook", e);
            }
        }

        calendarEventRepository.delete(calendarEvent);
    }

    public List<CalendarEvent> getCalendarEventsByReferenceId(UUID referenceId) {
        return calendarEventRepository.findByReferenceId(referenceId);
    }

    public List<CalendarEventDTO> getCalendarEvents(LocalDateTime startTime, LocalDateTime endTime) {
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

        return TeamCalendarDTO.builder()
                .teamLeaves(teamLeaves.stream()
                        .filter(leave -> "APPROVED".equals(leave.getStatus().getName()))
                        .map(leave -> {
                            User user = leave.getUser();

                            return LeaveRequestDTO.builder()
                                    .id(leave.getId())
                                    .userId(user.getId())
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
                .build();
    }

    private String createOutlookEvent(CalendarEventDTO eventDTO) {
        if (graphClient == null) {
            initializeGraphClient();
        }

        Event event = new Event();
        event.subject = eventDTO.getTitle();

        ItemBody body = new ItemBody();
        body.contentType = BodyType.TEXT;
        body.content = eventDTO.getDescription();
        event.body = body;

        DateTimeTimeZone start = new DateTimeTimeZone();
        start.dateTime = eventDTO.getStartTime().atZone(ZoneId.systemDefault())
                .format(DateTimeFormatter.ISO_OFFSET_DATE_TIME);
        start.timeZone = ZoneId.systemDefault().getId();
        event.start = start;

        DateTimeTimeZone end = new DateTimeTimeZone();
        end.dateTime = eventDTO.getEndTime().atZone(ZoneId.systemDefault())
                .format(DateTimeFormatter.ISO_OFFSET_DATE_TIME);
        end.timeZone = ZoneId.systemDefault().getId();
        event.end = end;

        Event createdEvent = graphClient.me().events()
                .buildRequest()
                .post(event);

        return createdEvent.id;
    }

    private void updateOutlookEvent(String outlookEventId, CalendarEventDTO eventDTO) {
        if (graphClient == null) {
            initializeGraphClient();
        }

        Event event = new Event();
        event.subject = eventDTO.getTitle();

        ItemBody body = new ItemBody();
        body.contentType = BodyType.TEXT;
        body.content = eventDTO.getDescription();
        event.body = body;

        DateTimeTimeZone start = new DateTimeTimeZone();
        start.dateTime = eventDTO.getStartTime().atZone(ZoneId.systemDefault())
                .format(DateTimeFormatter.ISO_OFFSET_DATE_TIME);
        start.timeZone = ZoneId.systemDefault().getId();
        event.start = start;

        DateTimeTimeZone end = new DateTimeTimeZone();
        end.dateTime = eventDTO.getEndTime().atZone(ZoneId.systemDefault())
                .format(DateTimeFormatter.ISO_OFFSET_DATE_TIME);
        end.timeZone = ZoneId.systemDefault().getId();
        event.end = end;

        graphClient.me().events(outlookEventId)
                .buildRequest()
                .patch(event);
    }

    private void deleteOutlookEvent(String outlookEventId) {
        if (graphClient == null) {
            initializeGraphClient();
        }

        graphClient.me().events(outlookEventId)
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