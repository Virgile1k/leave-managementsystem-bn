package com.leavemanagement.leave_management_system.controller;

import com.leavemanagement.leave_management_system.dto.LeaveReportDTO;
import com.leavemanagement.leave_management_system.dto.LeaveStatisticsDTO;
import com.leavemanagement.leave_management_system.service.ReportExportService;
import com.leavemanagement.leave_management_system.service.ReportStatisticsService;
import com.leavemanagement.leave_management_system.util.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/reports")
@RequiredArgsConstructor
public class ReportController {
    private final ReportStatisticsService reportStatisticsService;
    private final ReportExportService reportExportService;
    private final SecurityUtils securityUtils;

    /**
     * Get leave statistics for the current user
     */
    @GetMapping("/employee/statistics")
    public ResponseEntity<LeaveStatisticsDTO> getCurrentUserLeaveStatistics(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {

        UUID currentUserId = securityUtils.getCurrentUserId();
        LeaveStatisticsDTO statistics = reportStatisticsService.getEmployeeLeaveStatistics(
                currentUserId, startDate, endDate);

        return ResponseEntity.ok(statistics);
    }

    /**
     * Get leave statistics for a specific employee (managers & admins only)
     */
    @GetMapping("/employee/{userId}/statistics")
    @PreAuthorize("hasRole('MANAGER') or hasRole('ADMIN')")
    public ResponseEntity<LeaveStatisticsDTO> getEmployeeLeaveStatistics(
            @PathVariable UUID userId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {

        LeaveStatisticsDTO statistics = reportStatisticsService.getEmployeeLeaveStatistics(
                userId, startDate, endDate);

        return ResponseEntity.ok(statistics);
    }

    /**
     * Get leave statistics for a department (managers & admins only)
     */
    @GetMapping("/department/{departmentId}/statistics")
    @PreAuthorize("hasRole('MANAGER') or hasRole('ADMIN')")
    public ResponseEntity<LeaveStatisticsDTO> getDepartmentLeaveStatistics(
            @PathVariable UUID departmentId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {

        LeaveStatisticsDTO statistics = reportStatisticsService.getDepartmentLeaveStatistics(
                departmentId, startDate, endDate);

        return ResponseEntity.ok(statistics);
    }

    /**
     * Get leave statistics for a specific leave type (managers & admins only)
     */
    @GetMapping("/leave-type/{leaveTypeId}/statistics")
    @PreAuthorize("hasRole('MANAGER') or hasRole('ADMIN')")
    public ResponseEntity<LeaveStatisticsDTO> getLeaveTypeStatistics(
            @PathVariable UUID leaveTypeId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {

        LeaveStatisticsDTO statistics = reportStatisticsService.getLeaveTypeStatistics(
                leaveTypeId, startDate, endDate);

        return ResponseEntity.ok(statistics);
    }

    /**
     * Get company-wide leave statistics (admins only)
     */
    @GetMapping("/company/statistics")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<LeaveStatisticsDTO> getCompanyLeaveStatistics(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {

        LeaveStatisticsDTO statistics = reportStatisticsService.getCompanyLeaveStatistics(
                startDate, endDate);

        return ResponseEntity.ok(statistics);
    }

    /**
     * Get leave report data for a specific entity
     */
    @GetMapping("/{reportType}/{entityId}/data")
    @PreAuthorize("hasRole('MANAGER') or hasRole('ADMIN') or " +
            "(#reportType == 'EMPLOYEE' and #entityId == T(java.util.UUID).fromString(authentication.principal.userId))")
    public ResponseEntity<List<LeaveReportDTO>> getLeaveReportData(
            @PathVariable String reportType,
            @PathVariable UUID entityId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {

        List<LeaveReportDTO> reportData = reportStatisticsService.getLeaveReportData(
                reportType, entityId, startDate, endDate);

        return ResponseEntity.ok(reportData);
    }

    /**
     * Get company-wide leave report data (admins only)
     */
    @GetMapping("/company/data")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<LeaveReportDTO>> getCompanyReportData(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {

        List<LeaveReportDTO> reportData = reportStatisticsService.getLeaveReportData(
                "COMPANY", null, startDate, endDate);

        return ResponseEntity.ok(reportData);
    }
}