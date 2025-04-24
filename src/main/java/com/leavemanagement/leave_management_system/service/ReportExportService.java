package com.leavemanagement.leave_management_system.service;

import com.leavemanagement.leave_management_system.dto.LeaveReportDTO;
import com.leavemanagement.leave_management_system.dto.LeaveStatisticsDTO;
import com.leavemanagement.leave_management_system.dto.LeaveTypeSummaryDTO;
import lombok.RequiredArgsConstructor;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class ReportExportService {
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    /**
     * Export leave report data to CSV format
     */
    public String exportToCsv(List<LeaveReportDTO> reportData) {
        StringBuilder csv = new StringBuilder();

        // Add CSV header
        csv.append("Employee Name,Email,Department,Leave Type,Start Date,End Date,Status,Duration,Reason,Comments\n");

        // Add data rows
        for (LeaveReportDTO report : reportData) {
            csv.append(escapeSpecialCharacters(report.getEmployeeName())).append(",");
            csv.append(escapeSpecialCharacters(report.getEmployeeEmail())).append(",");
            csv.append(escapeSpecialCharacters(report.getDepartmentName())).append(",");
            csv.append(escapeSpecialCharacters(report.getLeaveTypeName())).append(",");
            csv.append(report.getStartDate().format(DATE_FORMATTER)).append(",");
            csv.append(report.getEndDate().format(DATE_FORMATTER)).append(",");
            csv.append(escapeSpecialCharacters(report.getStatus())).append(",");
            csv.append(report.getDuration()).append(",");
            csv.append(escapeSpecialCharacters(report.getReason())).append(",");
            csv.append(escapeSpecialCharacters(report.getComments() != null ? report.getComments() : "")).append("\n");
        }

        return csv.toString();
    }

    /**
     * Export leave report data to Excel format
     */
    public ByteArrayInputStream exportToExcel(List<LeaveReportDTO> reportData, LeaveStatisticsDTO statistics) throws IOException {
        try (XSSFWorkbook workbook = new XSSFWorkbook();
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {

            // Create details sheet
            Sheet detailsSheet = workbook.createSheet("Leave Details");

            // Create header row
            Row headerRow = detailsSheet.createRow(0);
            String[] columns = {"Employee Name", "Email", "Department", "Leave Type", "Start Date", "End Date",
                    "Status", "Duration (Days)", "Reason", "Comments"};

            CellStyle headerCellStyle = workbook.createCellStyle();
            headerCellStyle.setFillForegroundColor(IndexedColors.LIGHT_BLUE.getIndex());
            headerCellStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);

            Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerCellStyle.setFont(headerFont);

            for (int i = 0; i < columns.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(columns[i]);
                cell.setCellStyle(headerCellStyle);
            }

            // Create data rows
            int rowIdx = 1;
            for (LeaveReportDTO report : reportData) {
                Row row = detailsSheet.createRow(rowIdx++);

                row.createCell(0).setCellValue(report.getEmployeeName());
                row.createCell(1).setCellValue(report.getEmployeeEmail());
                row.createCell(2).setCellValue(report.getDepartmentName());
                row.createCell(3).setCellValue(report.getLeaveTypeName());
                row.createCell(4).setCellValue(report.getStartDate().format(DATE_FORMATTER));
                row.createCell(5).setCellValue(report.getEndDate().format(DATE_FORMATTER));
                row.createCell(6).setCellValue(report.getStatus());
                row.createCell(7).setCellValue(report.getDuration().doubleValue());
                row.createCell(8).setCellValue(report.getReason());
                row.createCell(9).setCellValue(report.getComments() != null ? report.getComments() : "");
            }

            // Auto-size columns
            for (int i = 0; i < columns.length; i++) {
                detailsSheet.autoSizeColumn(i);
            }

            // Create summary sheet if statistics are provided
            if (statistics != null) {
                createSummarySheet(workbook, statistics);
            }

            workbook.write(out);
            return new ByteArrayInputStream(out.toByteArray());
        }
    }

    /**
     * Create a summary sheet with statistics
     */
    private void createSummarySheet(XSSFWorkbook workbook, LeaveStatisticsDTO statistics) {
        Sheet summarySheet = workbook.createSheet("Summary");

        // Create styles
        CellStyle headerStyle = workbook.createCellStyle();
        Font headerFont = workbook.createFont();
        headerFont.setBold(true);
        headerStyle.setFont(headerFont);

        CellStyle titleStyle = workbook.createCellStyle();
        Font titleFont = workbook.createFont();
        titleFont.setBold(true);
        titleFont.setFontHeightInPoints((short) 14);
        titleStyle.setFont(titleFont);

        // Add title
        Row titleRow = summarySheet.createRow(0);
        Cell titleCell = titleRow.createCell(0);
        titleCell.setCellValue("Leave Statistics Report");
        titleCell.setCellStyle(titleStyle);

        // Add report metadata
        Row entityRow = summarySheet.createRow(2);
        entityRow.createCell(0).setCellValue("Entity:");
        Cell entityTypeCell = entityRow.createCell(1);
        entityTypeCell.setCellValue(statistics.getType() + ": " + statistics.getName());
        entityTypeCell.setCellStyle(headerStyle);

        Row periodRow = summarySheet.createRow(3);
        periodRow.createCell(0).setCellValue("Period:");
        Cell periodCell = periodRow.createCell(1);
        periodCell.setCellValue(statistics.getStartDate().format(DATE_FORMATTER) + " to " +
                statistics.getEndDate().format(DATE_FORMATTER));
        periodCell.setCellStyle(headerStyle);

        // Add summary metrics
        int rowIdx = 5;
        Row headerRow = summarySheet.createRow(rowIdx++);
        headerRow.createCell(0).setCellValue("Metric");
        headerRow.createCell(1).setCellValue("Value");
        headerRow.getCell(0).setCellStyle(headerStyle);
        headerRow.getCell(1).setCellStyle(headerStyle);

        Row totalRequestsRow = summarySheet.createRow(rowIdx++);
        totalRequestsRow.createCell(0).setCellValue("Total Leave Requests");
        totalRequestsRow.createCell(1).setCellValue(statistics.getTotalRequests());

        Row approvedRequestsRow = summarySheet.createRow(rowIdx++);
        approvedRequestsRow.createCell(0).setCellValue("Approved Leave Requests");
        approvedRequestsRow.createCell(1).setCellValue(statistics.getApprovedRequests());

        Row totalDaysRow = summarySheet.createRow(rowIdx++);
        totalDaysRow.createCell(0).setCellValue("Total Leave Days");
        totalDaysRow.createCell(1).setCellValue(statistics.getTotalLeaveDays().doubleValue());

        Row avgDurationRow = summarySheet.createRow(rowIdx++);
        avgDurationRow.createCell(0).setCellValue("Average Leave Duration");
        avgDurationRow.createCell(1).setCellValue(statistics.getAverageLeaveDuration().doubleValue());

        // Add leave type breakdown
        rowIdx += 2;
        Row breakdownHeaderRow = summarySheet.createRow(rowIdx++);
        breakdownHeaderRow.createCell(0).setCellValue("Leave Type Breakdown");
        breakdownHeaderRow.getCell(0).setCellStyle(headerStyle);

        Row breakdownColumnsRow = summarySheet.createRow(rowIdx++);
        breakdownColumnsRow.createCell(0).setCellValue("Leave Type");
        breakdownColumnsRow.createCell(1).setCellValue("Days");
        breakdownColumnsRow.createCell(2).setCellValue("Percentage");
        breakdownColumnsRow.getCell(0).setCellStyle(headerStyle);
        breakdownColumnsRow.getCell(1).setCellStyle(headerStyle);
        breakdownColumnsRow.getCell(2).setCellStyle(headerStyle);

        for (LeaveTypeSummaryDTO leaveType : statistics.getLeaveTypeBreakdown()) {
            Row leaveTypeRow = summarySheet.createRow(rowIdx++);
            leaveTypeRow.createCell(0).setCellValue(leaveType.getLeaveTypeName());
            leaveTypeRow.createCell(1).setCellValue(leaveType.getTotalDays().doubleValue());
            leaveTypeRow.createCell(2).setCellValue(leaveType.getPercentage().doubleValue() + "%");
        }

        // Add monthly distribution
        rowIdx += 2;
        Row monthlyHeaderRow = summarySheet.createRow(rowIdx++);
        monthlyHeaderRow.createCell(0).setCellValue("Monthly Distribution");
        monthlyHeaderRow.getCell(0).setCellStyle(headerStyle);

        Row monthlyColumnsRow = summarySheet.createRow(rowIdx++);
        monthlyColumnsRow.createCell(0).setCellValue("Month");
        monthlyColumnsRow.createCell(1).setCellValue("Days");
        monthlyColumnsRow.getCell(0).setCellStyle(headerStyle);
        monthlyColumnsRow.getCell(1).setCellStyle(headerStyle);

        for (Map.Entry<String, BigDecimal> entry : statistics.getMonthlyDistribution().entrySet()) {
            if (entry.getValue().compareTo(BigDecimal.ZERO) > 0) {
                Row monthRow = summarySheet.createRow(rowIdx++);
                monthRow.createCell(0).setCellValue(capitalizeMonth(entry.getKey()));
                monthRow.createCell(1).setCellValue(entry.getValue().doubleValue());
            }
        }

        // Auto-size columns
        summarySheet.autoSizeColumn(0);
        summarySheet.autoSizeColumn(1);
        summarySheet.autoSizeColumn(2);
    }

    /**
     * Helper method to capitalize month names
     */
    private String capitalizeMonth(String month) {
        String lower = month.toLowerCase();
        return lower.substring(0, 1).toUpperCase() + lower.substring(1);
    }

    /**
     * Escape special characters for CSV format
     */
    private String escapeSpecialCharacters(String data) {
        if (data == null) {
            return "";
        }

        String escapedData = data.replaceAll("\"", "\"\"");
        return "\"" + escapedData + "\"";
    }
}