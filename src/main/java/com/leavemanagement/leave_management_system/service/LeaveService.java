package com.leavemanagement.leave_management_system.service;
import com.leavemanagement.leave_management_system.dto.*;
import com.leavemanagement.leave_management_system.exceptions.ResourceNotFoundException;
import com.leavemanagement.leave_management_system.model.*;
import com.leavemanagement.leave_management_system.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.annotation.PostConstruct;
import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
    private final CalendarService calendarService;
    private final UserRepository userRepository;
    private final NotificationService notificationService;

    private static final Logger logger = LoggerFactory.getLogger(LeaveService.class);

    @PostConstruct
    public void initializeStatuses() {
        logger.info("Initializing leave request statuses");
        // Check if "PENDING" status exists, if not create it
        if (!leaveRequestStatusRepository.findByName("PENDING").isPresent()) {
            logger.info("Creating PENDING status");
            LeaveRequestStatus pendingStatus = new LeaveRequestStatus();
            pendingStatus.setName("PENDING");
            pendingStatus.setDescription("Leave request is pending approval");
            leaveRequestStatusRepository.save(pendingStatus);
        }

        // Add other required statuses
        if (!leaveRequestStatusRepository.findByName("APPROVED").isPresent()) {
            logger.info("Creating APPROVED status");
            LeaveRequestStatus approvedStatus = new LeaveRequestStatus();
            approvedStatus.setName("APPROVED");
            approvedStatus.setDescription("Leave request has been approved");
            leaveRequestStatusRepository.save(approvedStatus);
        }

        if (!leaveRequestStatusRepository.findByName("REJECTED").isPresent()) {
            logger.info("Creating REJECTED status");
            LeaveRequestStatus rejectedStatus = new LeaveRequestStatus();
            rejectedStatus.setName("REJECTED");
            rejectedStatus.setDescription("Leave request has been rejected");
            leaveRequestStatusRepository.save(rejectedStatus);
        }

        if (!leaveRequestStatusRepository.findByName("CANCELLED").isPresent()) {
            logger.info("Creating CANCELLED status");
            LeaveRequestStatus cancelledStatus = new LeaveRequestStatus();
            cancelledStatus.setName("CANCELLED");
            cancelledStatus.setDescription("Leave request has been cancelled");
            leaveRequestStatusRepository.save(cancelledStatus);
        }

        logger.info("Status initialization complete");
    }

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
        // Get user entity from repository
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

        // Try to find PENDING status, with fallback to create it if needed
        LeaveRequestStatus pendingStatus;
        Optional<LeaveRequestStatus> statusOpt = leaveRequestStatusRepository.findByName("PENDING");

        if (statusOpt.isPresent()) {
            pendingStatus = statusOpt.get();
        } else {
            logger.info("PENDING status not found during request creation, creating it now");
            pendingStatus = new LeaveRequestStatus();
            pendingStatus.setName("PENDING");
            pendingStatus.setDescription("Leave request is pending approval");
            pendingStatus = leaveRequestStatusRepository.save(pendingStatus);
        }

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
        LeaveRequestDTO savedRequestDTO = convertToLeaveRequestDTO(savedRequest);
        List<User> managers = userRepository.findManagersByDepartmentId(user.getDepartment().getId());
        notificationService.notifyLeaveRequestSubmitted(savedRequestDTO, managers);

        return savedRequestDTO;
    }

    @Transactional
    public LeaveRequestDTO updateLeaveRequestStatus(UUID requestId, LeaveRequestUpdateDTO updateDTO) {
        LeaveRequest leaveRequest = leaveRequestRepository.findById(requestId)
                .orElseThrow(() -> new ResourceNotFoundException("Leave request not found"));

        // Try to find status with fallback to create it if needed
        LeaveRequestStatus newStatus;
        Optional<LeaveRequestStatus> statusOpt = leaveRequestStatusRepository.findByName(updateDTO.getStatus());

        if (statusOpt.isPresent()) {
            newStatus = statusOpt.get();
        } else {
            logger.info("Status '{}' not found during status update, creating it now", updateDTO.getStatus());
            newStatus = new LeaveRequestStatus();
            newStatus.setName(updateDTO.getStatus());
            newStatus.setDescription(updateDTO.getStatus() + " status");
            newStatus = leaveRequestStatusRepository.save(newStatus);
        }

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
        LeaveRequestDTO updatedRequestDTO = convertToLeaveRequestDTO(updatedRequest);
        notificationService.notifyLeaveRequestUpdated(updatedRequestDTO, updateDTO.getStatus());

        return updatedRequestDTO;
    }

    @Transactional
    public LeaveBalanceDTO adjustLeaveBalance(LeaveBalanceAdjustmentDTO adjustmentDTO) {
        // Add debug logging
        logger.debug("Adjusting leave balance: {}", adjustmentDTO);

        // Default to current year if year is null
        int year = adjustmentDTO.getYear() != null
                ? adjustmentDTO.getYear()
                : LocalDate.now().getYear();

        LeaveBalance leaveBalance = leaveBalanceRepository
                .findByUserIdAndLeaveTypeIdAndYear(
                        adjustmentDTO.getUserId(),
                        adjustmentDTO.getLeaveTypeId(),
                        year)
                .orElseGet(() -> getOrCreateLeaveBalance(
                        adjustmentDTO.getUserId(),
                        adjustmentDTO.getLeaveTypeId(),
                        year));

        // Log the current state before changes
        logger.debug("Before adjustment - Balance: {}", leaveBalance);

        // Set adjustment days with null check
        BigDecimal adjustmentDays = adjustmentDTO.getAdjustmentDays() != null
                ? adjustmentDTO.getAdjustmentDays()
                : BigDecimal.ZERO;

        leaveBalance.setAdjustmentDays(adjustmentDays);

        // Recalculate total days
        BigDecimal newTotalDays = calculateTotalDays(leaveBalance);
        leaveBalance.setTotalDays(newTotalDays);

        // Log what we're about to save
        logger.debug("After adjustment - Balance with new total days: {}", leaveBalance);

        LeaveBalance savedBalance = leaveBalanceRepository.save(leaveBalance);

        // Make sure the save worked
        logger.debug("Saved balance from database: {}", savedBalance);

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
            // Get user entity from repository
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
        // Log entry point
        logger.debug("Calculating total days for balance: {}", leaveBalance);

        BigDecimal baseAccrual = leaveBalance.getLeaveType().getAccrualRate().multiply(new BigDecimal("12"));

        // For PTO, use standard 20 days
        if ("PTO".equalsIgnoreCase(leaveBalance.getLeaveType().getName())) {
            baseAccrual = new BigDecimal("20");
        }

        // Add adjustment days (with null check)
        BigDecimal adjustmentDays = leaveBalance.getAdjustmentDays() != null ?
                leaveBalance.getAdjustmentDays() : BigDecimal.ZERO;

        BigDecimal totalWithAdjustment = baseAccrual.add(adjustmentDays);

        // Log calculations
        logger.debug("Base accrual: {}, Adjustment: {}, Total before cap: {}",
                baseAccrual, adjustmentDays, totalWithAdjustment);

        // Cap at max days if applicable
        BigDecimal result;
        if (leaveBalance.getLeaveType().getMaxDays() != null &&
                totalWithAdjustment.compareTo(new BigDecimal(leaveBalance.getLeaveType().getMaxDays())) > 0) {
            result = new BigDecimal(leaveBalance.getLeaveType().getMaxDays());
            logger.debug("Capping at max days: {}", result);
        } else {
            result = totalWithAdjustment;
        }

        // Log final result
        logger.debug("Final total days: {}", result);
        return result;
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

        // For now, passing null as departmentId (global/company-wide event)
        UUID departmentId = null; // This creates a global event

        calendarService.createCalendarEvent(calendarEventDTO, departmentId, leaveRequest.getUser().getId());
    }

    @Transactional
    public LeaveTypeDTO createLeaveType(LeaveTypeCreateDTO createDTO) {
        LeaveType leaveType = LeaveType.builder()
                .name(createDTO.getName())
                .description(createDTO.getDescription())
                .accrualRate(createDTO.getAccrualRate())
                .requiresDoc(createDTO.getRequiresDoc())
                .maxDays(createDTO.getMaxDays())
                .isActive(true)
                .build();

        LeaveType savedLeaveType = leaveTypeRepository.save(leaveType);
        return convertToLeaveTypeDTO(savedLeaveType);
    }

    private void updateCalendarEvent(LeaveRequest leaveRequest) {
        List<CalendarEvent> events = calendarService.getCalendarEventsByReferenceId(leaveRequest.getId());

        if (!events.isEmpty()) {
            CalendarEvent event = events.get(0);

            // For now, passing null as departmentId (global/company-wide event)
            UUID departmentId = null; // This creates a global event

            // If request is rejected, delete the event
            if ("REJECTED".equals(leaveRequest.getStatus().getName())) {
                calendarService.deleteCalendarEvent(event.getId(), departmentId, leaveRequest.getUser().getId());
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

            calendarService.updateCalendarEvent(calendarEventDTO, departmentId, leaveRequest.getUser().getId());
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
        User user = leaveRequest.getUser();

        return LeaveRequestDTO.builder()
                .id(leaveRequest.getId())
                .userId(user.getId())
                .userName(user.getFullName())  // Add user name
                .email(user.getEmail())        // Add email
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