package com.leavemanagement.leave_management_system.service;

import com.leavemanagement.leave_management_system.dto.LeaveReportDTO;
import com.leavemanagement.leave_management_system.dto.LeaveStatisticsDTO;
import com.leavemanagement.leave_management_system.dto.LeaveTypeSummaryDTO;
import com.leavemanagement.leave_management_system.exceptions.ResourceNotFoundException;
import com.leavemanagement.leave_management_system.model.Department;
import com.leavemanagement.leave_management_system.model.LeaveRequest;
import com.leavemanagement.leave_management_system.model.LeaveType;
import com.leavemanagement.leave_management_system.model.User;
import com.leavemanagement.leave_management_system.repository.DepartmentRepository;
import com.leavemanagement.leave_management_system.repository.LeaveRequestRepository;
import com.leavemanagement.leave_management_system.repository.LeaveTypeRepository;
import com.leavemanagement.leave_management_system.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.Month;
import java.time.Year;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ReportStatisticsService {
    private final LeaveRequestRepository leaveRequestRepository;
    private final UserRepository userRepository;
    private final DepartmentRepository departmentRepository;
    private final LeaveTypeRepository leaveTypeRepository;

    /**
     * Generate leave statistics for a specific department
     */
    @Transactional(readOnly = true)
    public LeaveStatisticsDTO getDepartmentLeaveStatistics(UUID departmentId, LocalDate startDate, LocalDate endDate) {
        Department department = departmentRepository.findById(departmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Department not found"));

        List<LeaveRequest> leaveRequests = leaveRequestRepository.findByDepartmentAndDateRange(
                departmentId, startDate, endDate);

        return generateLeaveStatistics(leaveRequests, department.getName(), "DEPARTMENT", startDate, endDate);
    }

    /**
     * Generate leave statistics for a specific employee
     */
    @Transactional(readOnly = true)
    public LeaveStatisticsDTO getEmployeeLeaveStatistics(UUID userId, LocalDate startDate, LocalDate endDate) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        List<LeaveRequest> leaveRequests = leaveRequestRepository.findByUserId(userId).stream()
                .filter(lr -> isWithinDateRange(lr, startDate, endDate) && "APPROVED".equals(lr.getStatus().getName()))
                .collect(Collectors.toList());

        return generateLeaveStatistics(leaveRequests, user.getFullName(), "EMPLOYEE", startDate, endDate);
    }

    /**
     * Generate leave statistics for a specific leave type
     */
    @Transactional(readOnly = true)
    public LeaveStatisticsDTO getLeaveTypeStatistics(UUID leaveTypeId, LocalDate startDate, LocalDate endDate) {
        LeaveType leaveType = leaveTypeRepository.findById(leaveTypeId)
                .orElseThrow(() -> new ResourceNotFoundException("Leave type not found"));

        // We need to query all leave requests and filter by leave type since there's no direct repository method
        List<LeaveRequest> allLeaveRequests = leaveRequestRepository.findAll().stream()
                .filter(lr -> lr.getLeaveType().getId().equals(leaveTypeId) &&
                        isWithinDateRange(lr, startDate, endDate) &&
                        "APPROVED".equals(lr.getStatus().getName()))
                .collect(Collectors.toList());

        return generateLeaveStatistics(allLeaveRequests, leaveType.getName(), "LEAVE_TYPE", startDate, endDate);
    }

    /**
     * Generate company-wide leave statistics
     */
    @Transactional(readOnly = true)
    public LeaveStatisticsDTO getCompanyLeaveStatistics(LocalDate startDate, LocalDate endDate) {
        List<LeaveRequest> leaveRequests = leaveRequestRepository.findAll().stream()
                .filter(lr -> isWithinDateRange(lr, startDate, endDate) && "APPROVED".equals(lr.getStatus().getName()))
                .collect(Collectors.toList());

        return generateLeaveStatistics(leaveRequests, "Company", "COMPANY", startDate, endDate);
    }

    /**
     * Get detailed report data for export
     */
    @Transactional(readOnly = true)
    public List<LeaveReportDTO> getLeaveReportData(String reportType, UUID entityId, LocalDate startDate, LocalDate endDate) {
        List<LeaveRequest> leaveRequests;

        switch (reportType.toUpperCase()) {
            case "EMPLOYEE":
                leaveRequests = leaveRequestRepository.findByUserId(entityId).stream()
                        .filter(lr -> isWithinDateRange(lr, startDate, endDate))
                        .collect(Collectors.toList());
                break;
            case "DEPARTMENT":
                leaveRequests = leaveRequestRepository.findByDepartmentAndDateRange(entityId, startDate, endDate);
                break;
            case "LEAVE_TYPE":
                leaveRequests = leaveRequestRepository.findAll().stream()
                        .filter(lr -> lr.getLeaveType().getId().equals(entityId) &&
                                isWithinDateRange(lr, startDate, endDate))
                        .collect(Collectors.toList());
                break;
            case "COMPANY":
                leaveRequests = leaveRequestRepository.findAll().stream()
                        .filter(lr -> isWithinDateRange(lr, startDate, endDate))
                        .collect(Collectors.toList());
                break;
            default:
                throw new IllegalArgumentException("Invalid report type: " + reportType);
        }

        return leaveRequests.stream()
                .map(lr -> LeaveReportDTO.builder()
                        .id(lr.getId())
                        .employeeName(lr.getUser().getFullName())
                        .employeeEmail(lr.getUser().getEmail())
                        .departmentName(lr.getUser().getDepartment() != null ?
                                lr.getUser().getDepartment().getName() : "N/A")
                        .leaveTypeName(lr.getLeaveType().getName())
                        .startDate(lr.getStartDate())
                        .endDate(lr.getEndDate())
                        .status(lr.getStatus().getName())
                        .duration(calculateBusinessDays(lr.getStartDate(), lr.getEndDate()))
                        .reason(lr.getReason())
                        .comments(lr.getComments())
                        .build())
                .collect(Collectors.toList());
    }

    /**
     * Helper method to check if a leave request falls within the given date range
     */
    private boolean isWithinDateRange(LeaveRequest leaveRequest, LocalDate startDate, LocalDate endDate) {
        return (leaveRequest.getStartDate().isEqual(startDate) || leaveRequest.getStartDate().isAfter(startDate)) &&
                (leaveRequest.getEndDate().isEqual(endDate) || leaveRequest.getEndDate().isBefore(endDate)) ||
                (leaveRequest.getStartDate().isBefore(startDate) && leaveRequest.getEndDate().isAfter(endDate));
    }

    /**
     * Generate comprehensive leave statistics based on leave requests
     */
    private LeaveStatisticsDTO generateLeaveStatistics(List<LeaveRequest> leaveRequests, String name, String type,
                                                       LocalDate startDate, LocalDate endDate) {
        // Count total requests and approved requests
        long totalRequests = leaveRequests.size();
        long approvedRequests = leaveRequests.stream()
                .filter(lr -> "APPROVED".equals(lr.getStatus().getName()))
                .count();

        // Only consider approved requests for the rest of the statistics
        List<LeaveRequest> approvedLeaves = leaveRequests.stream()
                .filter(lr -> "APPROVED".equals(lr.getStatus().getName()))
                .collect(Collectors.toList());

        // Calculate total leave days
        BigDecimal totalLeaveDays = approvedLeaves.stream()
                .map(lr -> calculateBusinessDays(lr.getStartDate(), lr.getEndDate()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // Calculate average leave duration
        BigDecimal avgLeaveDuration = approvedLeaves.isEmpty() ? BigDecimal.ZERO :
                totalLeaveDays.divide(new BigDecimal(approvedLeaves.size()), 2, BigDecimal.ROUND_HALF_UP);

        // Group by leave type
        Map<String, BigDecimal> leaveTypeBreakdown = new HashMap<>();
        for (LeaveRequest lr : approvedLeaves) {
            String leaveTypeName = lr.getLeaveType().getName();
            BigDecimal duration = calculateBusinessDays(lr.getStartDate(), lr.getEndDate());

            leaveTypeBreakdown.put(
                    leaveTypeName,
                    leaveTypeBreakdown.getOrDefault(leaveTypeName, BigDecimal.ZERO).add(duration)
            );
        }

        // Convert to LeaveTypeSummaryDTO list
        List<LeaveTypeSummaryDTO> leaveTypeSummaries = leaveTypeBreakdown.entrySet().stream()
                .map(entry -> LeaveTypeSummaryDTO.builder()
                        .leaveTypeName(entry.getKey())
                        .totalDays(entry.getValue())
                        .percentage(totalLeaveDays.compareTo(BigDecimal.ZERO) > 0 ?
                                entry.getValue().multiply(new BigDecimal("100")).divide(totalLeaveDays, 2, BigDecimal.ROUND_HALF_UP) :
                                BigDecimal.ZERO)
                        .build())
                .collect(Collectors.toList());

        // Calculate monthly distribution
        Map<Month, BigDecimal> monthlyDistribution = new HashMap<>();
        for (LeaveRequest lr : approvedLeaves) {
            // For each day in the leave period, count it in the appropriate month
            LocalDate currentDate = lr.getStartDate();
            while (!currentDate.isAfter(lr.getEndDate())) {
                if (isBusinessDay(currentDate)) {
                    Month month = currentDate.getMonth();
                    monthlyDistribution.put(
                            month,
                            monthlyDistribution.getOrDefault(month, BigDecimal.ZERO).add(BigDecimal.ONE)
                    );
                }
                currentDate = currentDate.plusDays(1);
            }
        }

        // Create the final statistics DTO
        return LeaveStatisticsDTO.builder()
                .name(name)
                .type(type)
                .startDate(startDate)
                .endDate(endDate)
                .totalRequests(totalRequests)
                .approvedRequests(approvedRequests)
                .totalLeaveDays(totalLeaveDays)
                .averageLeaveDuration(avgLeaveDuration)
                .leaveTypeBreakdown(leaveTypeSummaries)
                .monthlyDistribution(convertMonthlyDistributionToMap(monthlyDistribution))
                .build();
    }

    /**
     * Helper method to calculate business days between two dates (excluding weekends)
     */
    private BigDecimal calculateBusinessDays(LocalDate startDate, LocalDate endDate) {
        BigDecimal businessDays = BigDecimal.ZERO;
        LocalDate currentDate = startDate;

        while (!currentDate.isAfter(endDate)) {
            if (isBusinessDay(currentDate)) {
                businessDays = businessDays.add(BigDecimal.ONE);
            }
            currentDate = currentDate.plusDays(1);
        }

        return businessDays;
    }

    /**
     * Check if a date is a business day (not weekend)
     */
    private boolean isBusinessDay(LocalDate date) {
        DayOfWeek dayOfWeek = date.getDayOfWeek();
        return !(dayOfWeek == DayOfWeek.SATURDAY || dayOfWeek == DayOfWeek.SUNDAY);
    }

    /**
     * Convert monthly distribution to a map of month names to values
     */
    private Map<String, BigDecimal> convertMonthlyDistributionToMap(Map<Month, BigDecimal> monthlyDistribution) {
        Map<String, BigDecimal> result = new LinkedHashMap<>();

        // Ensure all months are represented in order
        Arrays.stream(Month.values()).forEach(month -> {
            result.put(month.name(), monthlyDistribution.getOrDefault(month, BigDecimal.ZERO));
        });

        return result;
    }
}