package com.leavemanagement.leave_management_system.controller;

import com.leavemanagement.leave_management_system.dto.LeaveReportDTO;
import com.leavemanagement.leave_management_system.dto.LeaveStatisticsDTO;
import com.leavemanagement.leave_management_system.service.ReportExportService;
import com.leavemanagement.leave_management_system.service.ReportStatisticsService;
import com.leavemanagement.leave_management_system.util.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.InputStreamResource;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/reports/export") // Changed from /api/reports/export to match v1 API pattern
@RequiredArgsConstructor
public class ReportExportController {
    private final ReportExportService reportExportService;
    private final ReportStatisticsService reportStatisticsService;
    private final SecurityUtils securityUtils;

    private static final DateTimeFormatter FILE_DATE_FORMAT = DateTimeFormatter.ofPattern("yyyyMMdd");

    /**
     * Export leave report as CSV for a specific entity
     */
    @GetMapping("/{reportType}/{entityId}/csv")
    @PreAuthorize("hasRole('MANAGER') or hasRole('ADMIN') or " +
            "(#reportType == 'EMPLOYEE' and #entityId == T(java.util.UUID).fromString(authentication.principal.userId))")
    public ResponseEntity<String> exportReportToCsv(
            @PathVariable String reportType,
            @PathVariable UUID entityId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {

        List<LeaveReportDTO> reportData = reportStatisticsService.getLeaveReportData(
                reportType, entityId, startDate, endDate);

        String csvContent = reportExportService.exportToCsv(reportData);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType("text/csv"));
        headers.setContentDispositionFormData("attachment",
                String.format("leave_report_%s_%s_%s_%s.csv",
                        reportType.toLowerCase(),
                        entityId,
                        startDate.format(FILE_DATE_FORMAT),
                        endDate.format(FILE_DATE_FORMAT)));

        return ResponseEntity.ok()
                .headers(headers)
                .body(csvContent);
    }

    /**
     * Export leave report as Excel for a specific entity
     */
    @GetMapping("/{reportType}/{entityId}/excel")
    @PreAuthorize("hasRole('MANAGER') or hasRole('ADMIN') or " +
            "(#reportType == 'EMPLOYEE' and #entityId == T(java.util.UUID).fromString(authentication.principal.userId))")
    public ResponseEntity<InputStreamResource> exportReportToExcel(
            @PathVariable String reportType,
            @PathVariable UUID entityId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) throws IOException {

        List<LeaveReportDTO> reportData = reportStatisticsService.getLeaveReportData(
                reportType, entityId, startDate, endDate);

        LeaveStatisticsDTO statistics = null;
        switch (reportType.toUpperCase()) {
            case "EMPLOYEE":
                statistics = reportStatisticsService.getEmployeeLeaveStatistics(entityId, startDate, endDate);
                break;
            case "DEPARTMENT":
                statistics = reportStatisticsService.getDepartmentLeaveStatistics(entityId, startDate, endDate);
                break;
            case "LEAVE_TYPE":
                statistics = reportStatisticsService.getLeaveTypeStatistics(entityId, startDate, endDate);
                break;
            case "COMPANY":
                statistics = reportStatisticsService.getCompanyLeaveStatistics(startDate, endDate);
                break;
        }

        ByteArrayInputStream excelContent = reportExportService.exportToExcel(reportData, statistics);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"));
        headers.setContentDispositionFormData("attachment",
                String.format("leave_report_%s_%s_%s_%s.xlsx",
                        reportType.toLowerCase(),
                        entityId,
                        startDate.format(FILE_DATE_FORMAT),
                        endDate.format(FILE_DATE_FORMAT)));

        return ResponseEntity.ok()
                .headers(headers)
                .body(new InputStreamResource(excelContent));
    }

    /**
     * Export company-wide leave report as CSV (admins only)
     */
    @GetMapping("/company/csv")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<String> exportCompanyReportToCsv(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {

        List<LeaveReportDTO> reportData = reportStatisticsService.getLeaveReportData(
                "COMPANY", null, startDate, endDate);

        String csvContent = reportExportService.exportToCsv(reportData);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType("text/csv"));
        headers.setContentDispositionFormData("attachment",
                String.format("company_leave_report_%s_%s.csv",
                        startDate.format(FILE_DATE_FORMAT),
                        endDate.format(FILE_DATE_FORMAT)));

        return ResponseEntity.ok()
                .headers(headers)
                .body(csvContent);
    }

    /**
     * Export company-wide leave report as Excel (admins only)
     */
    @GetMapping("/company/excel")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<InputStreamResource> exportCompanyReportToExcel(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) throws IOException {

        List<LeaveReportDTO> reportData = reportStatisticsService.getLeaveReportData(
                "COMPANY", null, startDate, endDate);

        LeaveStatisticsDTO statistics = reportStatisticsService.getCompanyLeaveStatistics(startDate, endDate);

        ByteArrayInputStream excelContent = reportExportService.exportToExcel(reportData, statistics);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"));
        headers.setContentDispositionFormData("attachment",
                String.format("company_leave_report_%s_%s.xlsx",
                        startDate.format(FILE_DATE_FORMAT),
                        endDate.format(FILE_DATE_FORMAT)));

        return ResponseEntity.ok()
                .headers(headers)
                .body(new InputStreamResource(excelContent));
    }

    /**
     * Export current user's leave report as CSV
     */
    @GetMapping("/my-leaves/csv")
    public ResponseEntity<String> exportCurrentUserReportToCsv(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        UUID currentUserId = securityUtils.getCurrentUserId();

        // Added logging for debugging
        System.out.println("Exporting CSV report for user: " + currentUserId);

        List<LeaveReportDTO> reportData = reportStatisticsService.getLeaveReportData(
                "EMPLOYEE", currentUserId, startDate, endDate);

        String csvContent = reportExportService.exportToCsv(reportData);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType("text/csv"));
        headers.setContentDispositionFormData("attachment",
                String.format("my_leave_report_%s_%s.csv",
                        startDate.format(FILE_DATE_FORMAT),
                        endDate.format(FILE_DATE_FORMAT)));

        return ResponseEntity.ok()
                .headers(headers)
                .body(csvContent);
    }

    /**
     * Export current user's leave report as Excel
     */
    @GetMapping("/my-leaves/excel")
    public ResponseEntity<InputStreamResource> exportCurrentUserReportToExcel(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) throws IOException {

        UUID currentUserId = securityUtils.getCurrentUserId();

        // Added logging for debugging
        System.out.println("Exporting Excel report for user: " + currentUserId);

        List<LeaveReportDTO> reportData = reportStatisticsService.getLeaveReportData(
                "EMPLOYEE", currentUserId, startDate, endDate);

        LeaveStatisticsDTO statistics = reportStatisticsService.getEmployeeLeaveStatistics(
                currentUserId, startDate, endDate);

        ByteArrayInputStream excelContent = reportExportService.exportToExcel(reportData, statistics);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"));
        headers.setContentDispositionFormData("attachment",
                String.format("my_leave_report_%s_%s.xlsx",
                        startDate.format(FILE_DATE_FORMAT),
                        endDate.format(FILE_DATE_FORMAT)));

        return ResponseEntity.ok()
                .headers(headers)
                .body(new InputStreamResource(excelContent));
    }
}