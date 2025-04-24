//package com.leavemanagement.leave_management_system.controller;
//
//import com.leavemanagement.leave_management_system.dto.DocumentDTO;
//import com.leavemanagement.leave_management_system.service.DocumentService;
//import lombok.RequiredArgsConstructor;
//import org.springframework.http.HttpStatus;
//import org.springframework.http.ResponseEntity;
//import org.springframework.security.core.annotation.AuthenticationPrincipal;
//import org.springframework.security.oauth2.core.user.OAuth2User;
//import org.springframework.web.bind.annotation.*;
//import org.springframework.web.multipart.MultipartFile;
//
//import java.io.IOException;
//import java.util.List;
//import java.util.UUID;
//
//@RestController
//@RequestMapping("/api/v1/documents")
//@RequiredArgsConstructor
//public class DocumentController {
//
//    private final DocumentService documentService;
//
//    @PostMapping("/upload")
//    public ResponseEntity<DocumentDTO> uploadDocument(
//            @AuthenticationPrincipal OAuth2User principal,
//            @RequestParam("file") MultipartFile file,
//            @RequestParam(value = "leaveRequestId", required = false) UUID leaveRequestId) {
//
//        try {
//            UUID userId = UUID.fromString(principal.getAttribute("sub"));
//            DocumentDTO documentDTO = documentService.uploadDocument(userId, file, leaveRequestId);
//            return new ResponseEntity<>(documentDTO, HttpStatus.CREATED);
//        } catch (IOException e) {
//            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
//        }
//    }
//
//    @DeleteMapping("/{documentId}")
//    public ResponseEntity<Void> deleteDocument(
//            @AuthenticationPrincipal OAuth2User principal,
//            @PathVariable UUID documentId) {
//
//        UUID userId = UUID.fromString(principal.getAttribute("sub"));
//        documentService.deleteDocument(documentId, userId);
//        return ResponseEntity.noContent().build();
//    }
//
//    @GetMapping
//    public ResponseEntity<List<DocumentDTO>> getUserDocuments(
//            @AuthenticationPrincipal OAuth2User principal) {
//
//        UUID userId = UUID.fromString(principal.getAttribute("sub"));
//        return ResponseEntity.ok(documentService.getUserDocuments(userId));
//    }
//
//    @GetMapping("/leave-request/{leaveRequestId}")
//    public ResponseEntity<List<DocumentDTO>> getLeaveRequestDocuments(
//            @PathVariable UUID leaveRequestId) {
//
//        return ResponseEntity.ok(documentService.getLeaveRequestDocuments(leaveRequestId));
//    }
//}