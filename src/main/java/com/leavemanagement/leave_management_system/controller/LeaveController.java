package com.leavemanagement.leave_management_system.controller;

import com.leavemanagement.leave_management_system.dto.*;
import com.leavemanagement.leave_management_system.service.DocumentService;
import com.leavemanagement.leave_management_system.service.LeaveService;
import com.leavemanagement.leave_management_system.util.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@RestController
@RequestMapping("/api/v1/leaves")
@RequiredArgsConstructor
public class LeaveController {
    private final LeaveService leaveService;
    private final DocumentService documentService;
    private final SecurityUtils securityUtils;

    // ADMIN only can create leave types
    @PostMapping("/types")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<LeaveTypeDTO> createLeaveType(@RequestBody LeaveTypeCreateDTO createDTO) {
        return new ResponseEntity<>(leaveService.createLeaveType(createDTO), HttpStatus.CREATED);
    }

    // All users can view leave types
    @GetMapping("/types")
    @PreAuthorize("hasAnyRole('STAFF', 'MANAGER', 'ADMIN')")
    public ResponseEntity<List<LeaveTypeDTO>> getAllLeaveTypes() {
        return ResponseEntity.ok(leaveService.getAllLeaveTypes());
    }

    // Users can view their own leave balances
    @GetMapping("/balances")
    @PreAuthorize("hasAnyRole('STAFF', 'MANAGER', 'ADMIN')")
    public ResponseEntity<List<LeaveBalanceDTO>> getUserLeaveBalances() {
        UUID userId = securityUtils.getCurrentUserId();
        return ResponseEntity.ok(leaveService.getUserLeaveBalances(userId));
    }

    // Users can view their own leave requests
    @GetMapping("/requests")
    @PreAuthorize("hasAnyRole('STAFF', 'MANAGER', 'ADMIN')")
    public ResponseEntity<List<LeaveRequestDTO>> getUserLeaveRequests() {
        UUID userId = securityUtils.getCurrentUserId();
        return ResponseEntity.ok(leaveService.getUserLeaveRequests(userId));
    }

    // Users can create leave requests
    @PostMapping("/requests")
    @PreAuthorize("hasAnyRole('STAFF', 'MANAGER', 'ADMIN')")
    public ResponseEntity<LeaveRequestDTO> createLeaveRequest(@RequestBody LeaveRequestCreateDTO createDTO) {
        UUID userId = securityUtils.getCurrentUserId();
        return new ResponseEntity<>(leaveService.createLeaveRequest(userId, createDTO), HttpStatus.CREATED);
    }

    @PostMapping(value = "/requests/with-documents", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAnyRole('STAFF', 'MANAGER', 'ADMIN')")
    public ResponseEntity<LeaveRequestDTO> createLeaveRequestWithDocuments(
            @RequestPart("leaveRequest") LeaveRequestCreateDTO createDTO,
            @RequestPart(value = "files", required = false) List<MultipartFile> files) {

        UUID userId = securityUtils.getCurrentUserId();

        try {
            // First create the leave request without documents
            LeaveRequestDTO leaveRequestDTO = leaveService.createLeaveRequest(userId, createDTO);

            // Then upload and associate documents if any
            if (files != null && !files.isEmpty()) {
                List<UUID> documentIds = new ArrayList<>();

                for (MultipartFile file : files) {
                    try {
                        // Use the DocumentService to upload to S3 and create document records
                        DocumentDTO documentDTO = documentService.uploadDocument(userId, file, leaveRequestDTO.getId());
                        documentIds.add(documentDTO.getId());
                    } catch (IOException e) {
//                        logger.error("Error uploading document for leave request: {}", leaveRequestDTO.getId(), e);
                        // Continue processing other files
                    }
                }

                // If documents were successfully uploaded, retrieve the updated leave request
                if (!documentIds.isEmpty()) {
                    return ResponseEntity.ok(leaveService.getLeaveRequest(leaveRequestDTO.getId()));
                }
            }

            return new ResponseEntity<>(leaveRequestDTO, HttpStatus.CREATED);
        } catch (Exception e) {
//            logger.error("Error creating leave request with documents", e);
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    // Users can view a specific leave request (with checks for authorization in service)
    @GetMapping("/requests/{requestId}")
    @PreAuthorize("hasAnyRole('STAFF', 'MANAGER', 'ADMIN')")
    public ResponseEntity<LeaveRequestDTO> getLeaveRequest(@PathVariable UUID requestId) {
        return ResponseEntity.ok(leaveService.getLeaveRequest(requestId));
    }

    // Only managers and admins can update leave request status
    @PutMapping("/requests/{requestId}")
    @PreAuthorize("hasAnyRole('MANAGER', 'ADMIN')")
    public ResponseEntity<LeaveRequestDTO> updateLeaveRequestStatus(
            @PathVariable UUID requestId,
            @RequestBody LeaveRequestUpdateDTO updateDTO) {
        return ResponseEntity.ok(leaveService.updateLeaveRequestStatus(requestId, updateDTO));
    }

    // Only managers and admins can view pending approvals
    @GetMapping("/approvals")
    @PreAuthorize("hasAnyRole('MANAGER', 'ADMIN')")
    public ResponseEntity<List<LeaveRequestDTO>> getPendingApprovals() {
        return ResponseEntity.ok(leaveService.getPendingApprovals());
    }

    // Only admins can adjust leave balances
    @PostMapping("/balances/adjust")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<LeaveBalanceDTO> adjustLeaveBalance(@RequestBody LeaveBalanceAdjustmentDTO adjustmentDTO) {
//        logger.debug("Received adjustment request: {}", adjustmentDTO);
        return ResponseEntity.ok(leaveService.adjustLeaveBalance(adjustmentDTO));
    }
}