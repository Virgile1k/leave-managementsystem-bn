package com.leavemanagement.leave_management_system.service;
import com.leavemanagement.leave_management_system.dto.*;
import com.leavemanagement.leave_management_system.exceptions.ResourceNotFoundException;
import com.leavemanagement.leave_management_system.model.*;
import com.leavemanagement.leave_management_system.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class LeaveService {
    private final LeaveRequestRepository leaveRequestRepository;
    private final LeaveTypeRepository leaveTypeRepository;
    private final LeaveBalanceRepository leaveBalanceRepository;
    private final LeaveRequestStatusRepository leaveRequestStatusRepository;
    private final DocumentRepository documentRepository;
    private final HolidayRepository holidayRepository;
    private final UserService userService;
    private final NotificationService notificationService;
    private final CalendarService calendarService;
    private final UserRepository userRepository; // Added to fetch User entities directly

    public List<LeaveTypeDTO> getAllLeaveTypes() {
        return leaveTypeRepository.findByIsActiveTrue().stream()
                .map(this::convertToLeaveTypeDTO)
                .collect(Collectors.toList());
    }

    public List<LeaveBalanceDTO> getUserLeaveBalances(UUID userId) {
        int currentYear = LocalDate.now().getYear();
        return leaveBalanceRepository.findByUserIdAndYear(userId, currentYear).stream()
                .map(this::convertToLeaveBalanceDTO)
                .collect(Collectors.toList());
    }

    public List<LeaveRequestDTO> getUserLeaveRequests(UUID userId) {
        return leaveRequestRepository.findByUserId(userId).stream()
                .map(this::convertToLeaveRequestDTO)
                .collect(Collectors.toList());
    }

    @Transactional
    public LeaveRequestDTO createLeaveRequest(UUID userId, LeaveRequestCreateDTO createDTO) {
        // Get user entity from repository instead of using the non-existent method
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        LeaveType leaveType = leaveTypeRepository.findById(createDTO.getLeaveTypeId())
                .orElseThrow(() -> new ResourceNotFoundException("LeaveType not found"));

        // Calculate business days (excluding weekends and holidays)
        BigDecimal leaveDuration = calculateBusinessDays(createDTO.getStartDate(), createDTO.getEndDate());

        // Check leave balance
        LeaveBalance leaveBalance = getOrCreateLeaveBalance(userId, leaveType.getId(), createDTO.getStartDate().getYear());

        BigDecimal availableBalance = leaveBalance.getTotalDays()
                .subtract(leaveBalance.getUsedDays())
                .subtract(leaveBalance.getPendingDays());

        if (availableBalance.compareTo(leaveDuration) < 0) {
            throw new IllegalStateException("Insufficient leave balance");
        }

        // Update pending days
        leaveBalance.setPendingDays(leaveBalance.getPendingDays().add(leaveDuration));
        leaveBalanceRepository.save(leaveBalance);

        // Get pending status
        LeaveRequestStatus pendingStatus = leaveRequestStatusRepository.findByName("PENDING")
                .orElseThrow(() -> new ResourceNotFoundException("Status not found"));

        // Create leave request
        LeaveRequest leaveRequest = LeaveRequest.builder()
                .user(user)
                .leaveType(leaveType)
                .startDate(createDTO.getStartDate())
                .endDate(createDTO.getEndDate())
                .status(pendingStatus)
                .reason(createDTO.getReason())
                .fullDay(createDTO.getFullDay())
                .build();

        LeaveRequest savedRequest = leaveRequestRepository.save(leaveRequest);

        // Associate documents if any
        if (createDTO.getDocumentIds() != null && !createDTO.getDocumentIds().isEmpty()) {
            createDTO.getDocumentIds().forEach(docId -> {
                Document doc = documentRepository.findById(docId)
                        .orElseThrow(() -> new ResourceNotFoundException("Document not found"));
                doc.setLeaveRequest(savedRequest);
                documentRepository.save(doc);
            });
        }

        // Create calendar event
        createCalendarEvent(savedRequest);

        // Send notifications
        notificationService.sendLeaveRequestNotifications(savedRequest);

        return convertToLeaveRequestDTO(leaveRequestRepository.findById(savedRequest.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Leave request not found")));
    }

    @Transactional
    public LeaveRequestDTO updateLeaveRequestStatus(UUID requestId, LeaveRequestUpdateDTO updateDTO) {
        LeaveRequest leaveRequest = leaveRequestRepository.findById(requestId)
                .orElseThrow(() -> new ResourceNotFoundException("Leave request not found"));

        LeaveRequestStatus newStatus = leaveRequestStatusRepository.findByName(updateDTO.getStatus())
                .orElseThrow(() -> new ResourceNotFoundException("Status not found"));

        LeaveRequestStatus oldStatus = leaveRequest.getStatus();
        leaveRequest.setStatus(newStatus);
        leaveRequest.setComments(updateDTO.getComments());

        // Update leave balance
        LeaveBalance leaveBalance = leaveBalanceRepository
                .findByUserIdAndLeaveTypeIdAndYear(
                        leaveRequest.getUser().getId(),
                        leaveRequest.getLeaveType().getId(),
                        leaveRequest.getStartDate().getYear())
                .orElseThrow(() -> new ResourceNotFoundException("Leave balance not found"));

        BigDecimal leaveDuration = calculateBusinessDays(leaveRequest.getStartDate(), leaveRequest.getEndDate());

        if ("APPROVED".equals(updateDTO.getStatus()) && !"APPROVED".equals(oldStatus.getName())) {
            // If previously pending, remove from pending and add to used
            leaveBalance.setPendingDays(leaveBalance.getPendingDays().subtract(leaveDuration));
            leaveBalance.setUsedDays(leaveBalance.getUsedDays().add(leaveDuration));
        } else if ("REJECTED".equals(updateDTO.getStatus()) && !"REJECTED".equals(oldStatus.getName())) {
            // If previously pending or approved, adjust accordingly
            if ("PENDING".equals(oldStatus.getName())) {
                leaveBalance.setPendingDays(leaveBalance.getPendingDays().subtract(leaveDuration));
            } else if ("APPROVED".equals(oldStatus.getName())) {
                leaveBalance.setUsedDays(leaveBalance.getUsedDays().subtract(leaveDuration));
            }
        }

        leaveBalanceRepository.save(leaveBalance);
        LeaveRequest updatedRequest = leaveRequestRepository.save(leaveRequest);

        // Update calendar event
        updateCalendarEvent(updatedRequest);

        // Send notifications
        notificationService.sendLeaveStatusUpdateNotifications(updatedRequest);

        return convertToLeaveRequestDTO(updatedRequest);
    }

    @Transactional
    public LeaveBalanceDTO adjustLeaveBalance(LeaveBalanceAdjustmentDTO adjustmentDTO) {
        LeaveBalance leaveBalance = leaveBalanceRepository
                .findByUserIdAndLeaveTypeIdAndYear(
                        adjustmentDTO.getUserId(),
                        adjustmentDTO.getLeaveTypeId(),
                        adjustmentDTO.getYear())
                .orElseGet(() -> getOrCreateLeaveBalance(
                        adjustmentDTO.getUserId(),
                        adjustmentDTO.getLeaveTypeId(),
                        adjustmentDTO.getYear()));

        leaveBalance.setAdjustmentDays(adjustmentDTO.getAdjustmentDays());
        leaveBalance.setTotalDays(calculateTotalDays(leaveBalance));

        LeaveBalance savedBalance = leaveBalanceRepository.save(leaveBalance);
        return convertToLeaveBalanceDTO(savedBalance);
    }

    public List<LeaveRequestDTO> getPendingApprovals() {
        return leaveRequestRepository.findByStatus("PENDING").stream()
                .map(this::convertToLeaveRequestDTO)
                .collect(Collectors.toList());
    }

    public LeaveRequestDTO getLeaveRequest(UUID requestId) {
        return convertToLeaveRequestDTO(leaveRequestRepository.findById(requestId)
                .orElseThrow(() -> new ResourceNotFoundException("Leave request not found")));
    }

    private BigDecimal calculateBusinessDays(LocalDate startDate, LocalDate endDate) {
        BigDecimal businessDays = BigDecimal.ZERO;
        LocalDate currentDate = startDate;

        // Get holidays between the dates
        List<Holiday> holidays = holidayRepository.findHolidaysBetweenDates(startDate, endDate);

        while (!currentDate.isAfter(endDate)) {
            // Skip weekends
            if (!(currentDate.getDayOfWeek() == DayOfWeek.SATURDAY ||
                    currentDate.getDayOfWeek() == DayOfWeek.SUNDAY)) {
                // Skip holidays
                LocalDate finalCurrentDate = currentDate;
                boolean isHoliday = holidays.stream()
                        .anyMatch(holiday -> holiday.getDate().isEqual(finalCurrentDate));

                if (!isHoliday) {
                    businessDays = businessDays.add(BigDecimal.ONE);
                }
            }
            currentDate = currentDate.plusDays(1);
        }

        return businessDays;
    }

    private LeaveBalance getOrCreateLeaveBalance(UUID userId, UUID leaveTypeId, int year) {
        Optional<LeaveBalance> existingBalance = leaveBalanceRepository
                .findByUserIdAndLeaveTypeIdAndYear(userId, leaveTypeId, year);

        if (existingBalance.isPresent()) {
            return existingBalance.get();
        } else {
            // Get user entity from repository instead of using getUserEntityById
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new ResourceNotFoundException("User not found"));

            LeaveType leaveType = leaveTypeRepository.findById(leaveTypeId)
                    .orElseThrow(() -> new ResourceNotFoundException("LeaveType not found"));

            // Calculate total days based on accrual rate
            BigDecimal totalDays = leaveType.getAccrualRate().multiply(new BigDecimal("12")); // Monthly accrual * 12

            // For PTO, use standard 20 days per year
            if ("PTO".equalsIgnoreCase(leaveType.getName())) {
                totalDays = new BigDecimal("20");
            }

            // Cap at max days if applicable
            if (leaveType.getMaxDays() != null &&
                    totalDays.compareTo(new BigDecimal(leaveType.getMaxDays())) > 0) {
                totalDays = new BigDecimal(leaveType.getMaxDays());
            }

            LeaveBalance newBalance = LeaveBalance.builder()
                    .user(user)
                    .leaveType(leaveType)
                    .year(year)
                    .totalDays(totalDays)
                    .usedDays(BigDecimal.ZERO)
                    .pendingDays(BigDecimal.ZERO)
                    .adjustmentDays(BigDecimal.ZERO)
                    .build();

            return leaveBalanceRepository.save(newBalance);
        }
    }

    private BigDecimal calculateTotalDays(LeaveBalance leaveBalance) {
        BigDecimal baseAccrual = leaveBalance.getLeaveType().getAccrualRate().multiply(new BigDecimal("12"));

        // For PTO, use standard 20 days
        if ("PTO".equalsIgnoreCase(leaveBalance.getLeaveType().getName())) {
            baseAccrual = new BigDecimal("20");
        }

        // Add adjustment days
        BigDecimal totalWithAdjustment = baseAccrual.add(leaveBalance.getAdjustmentDays());

        // Cap at max days if applicable
        if (leaveBalance.getLeaveType().getMaxDays() != null &&
                totalWithAdjustment.compareTo(new BigDecimal(leaveBalance.getLeaveType().getMaxDays())) > 0) {
            return new BigDecimal(leaveBalance.getLeaveType().getMaxDays());
        }

        return totalWithAdjustment;
    }

    private void createCalendarEvent(LeaveRequest leaveRequest) {
        String title = leaveRequest.getUser().getFullName() + " - " + leaveRequest.getLeaveType().getName();

        LocalDateTime startDateTime = leaveRequest.getStartDate().atStartOfDay();
        LocalDateTime endDateTime = leaveRequest.getEndDate().atTime(23, 59, 59);

        CalendarEventDTO calendarEventDTO = CalendarEventDTO.builder()
                .title(title)
                .description(leaveRequest.getReason())
                .startTime(startDateTime)
                .endTime(endDateTime)
                .eventType("LEAVE")
                .referenceId(leaveRequest.getId())
                .build();

        calendarService.createCalendarEvent(calendarEventDTO);
    }

    private void updateCalendarEvent(LeaveRequest leaveRequest) {
        List<CalendarEvent> events = calendarService.getCalendarEventsByReferenceId(leaveRequest.getId());

        if (!events.isEmpty()) {
            CalendarEvent event = events.get(0);

            // If request is rejected, delete the event
            if ("REJECTED".equals(leaveRequest.getStatus().getName())) {
                calendarService.deleteCalendarEvent(event.getId());
                return;
            }

            // Otherwise update the event
            String title = leaveRequest.getUser().getFullName() + " - " + leaveRequest.getLeaveType().getName();

            CalendarEventDTO calendarEventDTO = CalendarEventDTO.builder()
                    .id(event.getId())
                    .title(title)
                    .description(leaveRequest.getReason())
                    .startTime(leaveRequest.getStartDate().atStartOfDay())
                    .endTime(leaveRequest.getEndDate().atTime(23, 59, 59))
                    .eventType("LEAVE")
                    .referenceId(leaveRequest.getId())
                    .outlookEventId(event.getOutlookEventId())
                    .build();

            calendarService.updateCalendarEvent(calendarEventDTO);
        }
    }

    private LeaveRequestDTO convertToLeaveRequestDTO(LeaveRequest leaveRequest) {
        List<DocumentDTO> documentDTOs = leaveRequest.getDocuments() != null ?
                leaveRequest.getDocuments().stream()
                        .map(doc -> DocumentDTO.builder()
                                .id(doc.getId())
                                .filename(doc.getFilename())
                                .fileUrl(doc.getFileUrl())
                                .fileType(doc.getFileType())
                                .build())
                        .collect(Collectors.toList()) :
                List.of();

        BigDecimal leaveDuration = calculateBusinessDays(leaveRequest.getStartDate(), leaveRequest.getEndDate());

        return LeaveRequestDTO.builder()
                .id(leaveRequest.getId())
                .userId(leaveRequest.getUser().getId())
                .leaveTypeId(leaveRequest.getLeaveType().getId())
                .leaveTypeName(leaveRequest.getLeaveType().getName())
                .startDate(leaveRequest.getStartDate())
                .endDate(leaveRequest.getEndDate())
                .status(leaveRequest.getStatus().getName())
                .reason(leaveRequest.getReason())
                .fullDay(leaveRequest.getFullDay())
                .comments(leaveRequest.getComments())
                .documents(documentDTOs)
                .leaveDuration(leaveDuration)
                .build();
    }

    private LeaveBalanceDTO convertToLeaveBalanceDTO(LeaveBalance leaveBalance) {
        BigDecimal availableDays = leaveBalance.getTotalDays()
                .subtract(leaveBalance.getUsedDays())
                .subtract(leaveBalance.getPendingDays());

        return LeaveBalanceDTO.builder()
                .id(leaveBalance.getId())
                .userId(leaveBalance.getUser().getId())
                .leaveTypeId(leaveBalance.getLeaveType().getId())
                .leaveTypeName(leaveBalance.getLeaveType().getName())
                .year(leaveBalance.getYear())
                .totalDays(leaveBalance.getTotalDays())
                .usedDays(leaveBalance.getUsedDays())
                .pendingDays(leaveBalance.getPendingDays())
                .adjustmentDays(leaveBalance.getAdjustmentDays())
                .availableDays(availableDays)
                .build();
    }

    private LeaveTypeDTO convertToLeaveTypeDTO(LeaveType leaveType) {
        return LeaveTypeDTO.builder()
                .id(leaveType.getId())
                .name(leaveType.getName())
                .description(leaveType.getDescription())
                .accrualRate(leaveType.getAccrualRate())
                .requiresDoc(leaveType.getRequiresDoc())
                .maxDays(leaveType.getMaxDays())
                .isActive(leaveType.getIsActive())
                .build();
    }
}