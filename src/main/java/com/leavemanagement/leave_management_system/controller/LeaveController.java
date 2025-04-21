package com.leavemanagement.leave_management_system.controller;

import com.leavemanagement.leave_management_system.dto.*;
import com.leavemanagement.leave_management_system.service.DocumentService;
import com.leavemanagement.leave_management_system.service.LeaveService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/leaves")
@RequiredArgsConstructor
public class LeaveController {
    private final LeaveService leaveService;
    private final DocumentService documentService;

    @GetMapping("/types")
    public ResponseEntity<List<LeaveTypeDTO>> getAllLeaveTypes() {
        return ResponseEntity.ok(leaveService.getAllLeaveTypes());
    }

    @GetMapping("/balances")
    public ResponseEntity<List<LeaveBalanceDTO>> getUserLeaveBalances(@AuthenticationPrincipal OAuth2User principal) {
        UUID userId = UUID.fromString(principal.getAttribute("sub"));
        return ResponseEntity.ok(leaveService.getUserLeaveBalances(userId));
    }

    @GetMapping("/requests")
    public ResponseEntity<List<LeaveRequestDTO>> getUserLeaveRequests(@AuthenticationPrincipal OAuth2User principal) {
        UUID userId = UUID.fromString(principal.getAttribute("sub"));
        return ResponseEntity.ok(leaveService.getUserLeaveRequests(userId));
    }

    @PostMapping("/requests")
    public ResponseEntity<LeaveRequestDTO> createLeaveRequest(
            @AuthenticationPrincipal OAuth2User principal,
            @RequestBody LeaveRequestCreateDTO createDTO) {
        UUID userId = UUID.fromString(principal.getAttribute("sub"));
        return new ResponseEntity<>(leaveService.createLeaveRequest(userId, createDTO), HttpStatus.CREATED);
    }

    // New endpoint to handle form data with file uploads
    @PostMapping(value = "/requests/with-documents", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<LeaveRequestDTO> createLeaveRequestWithDocuments(
            @AuthenticationPrincipal OAuth2User principal,
            @RequestPart("leaveRequest") LeaveRequestCreateDTO createDTO,
            @RequestPart(value = "files", required = false) List<MultipartFile> files) {

        UUID userId = UUID.fromString(principal.getAttribute("sub"));

        // First create the leave request without documents
        LeaveRequestDTO leaveRequestDTO = leaveService.createLeaveRequest(userId, createDTO);

        // Then upload and associate documents if any
        if (files != null && !files.isEmpty()) {
            List<UUID> documentIds = new ArrayList<>();

            for (MultipartFile file : files) {
                try {
                    DocumentDTO documentDTO = documentService.uploadDocument(userId, file, leaveRequestDTO.getId());
                    documentIds.add(documentDTO.getId());
                } catch (IOException e) {
                    // Log the error and continue with the next file
                    // In a production system, you might want to handle this differently
                    // such as rolling back the transaction or notifying the user
                }
            }

            // If documents were successfully uploaded, retrieve the updated leave request
            if (!documentIds.isEmpty()) {
                return ResponseEntity.ok(leaveService.getLeaveRequest(leaveRequestDTO.getId()));
            }
        }

        return new ResponseEntity<>(leaveRequestDTO, HttpStatus.CREATED);
    }

    @GetMapping("/requests/{requestId}")
    public ResponseEntity<LeaveRequestDTO> getLeaveRequest(@PathVariable UUID requestId) {
        return ResponseEntity.ok(leaveService.getLeaveRequest(requestId));
    }

    @PutMapping("/requests/{requestId}")
    public ResponseEntity<LeaveRequestDTO> updateLeaveRequestStatus(
            @PathVariable UUID requestId,
            @RequestBody LeaveRequestUpdateDTO updateDTO) {
        return ResponseEntity.ok(leaveService.updateLeaveRequestStatus(requestId, updateDTO));
    }

    @GetMapping("/approvals")
    public ResponseEntity<List<LeaveRequestDTO>> getPendingApprovals() {
        return ResponseEntity.ok(leaveService.getPendingApprovals());
    }

    @PostMapping("/balances/adjust")
    public ResponseEntity<LeaveBalanceDTO> adjustLeaveBalance(@RequestBody LeaveBalanceAdjustmentDTO adjustmentDTO) {
        return ResponseEntity.ok(leaveService.adjustLeaveBalance(adjustmentDTO));
    }
}