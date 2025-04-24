package com.leavemanagement.leave_management_system.service;

import com.leavemanagement.leave_management_system.dto.DocumentDTO;
import com.leavemanagement.leave_management_system.dto.FileUploadResultDTO;
import com.leavemanagement.leave_management_system.exceptions.ResourceNotFoundException;
import com.leavemanagement.leave_management_system.model.Document;
import com.leavemanagement.leave_management_system.model.LeaveRequest;
import com.leavemanagement.leave_management_system.model.User;
import com.leavemanagement.leave_management_system.repository.DocumentRepository;
import com.leavemanagement.leave_management_system.repository.LeaveRequestRepository;
import com.leavemanagement.leave_management_system.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class DocumentService {
    private final DocumentRepository documentRepository;
    private final UserRepository userRepository;
    private final LeaveRequestRepository leaveRequestRepository;
    private final FileStorageService fileStorageService;

    private static final Logger logger = LoggerFactory.getLogger(DocumentService.class);

    /**
     * Uploads a document for a leave request to S3
     */
    @Transactional
    public DocumentDTO uploadDocument(UUID userId, MultipartFile file, UUID leaveRequestId) throws IOException {
        logger.info("Uploading document for user: {} and leave request: {}", userId, leaveRequestId);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        LeaveRequest leaveRequest = null;
        if (leaveRequestId != null) {
            leaveRequest = leaveRequestRepository.findById(leaveRequestId)
                    .orElseThrow(() -> new ResourceNotFoundException("Leave request not found"));
        }

        // Upload file to S3 and get the URL and key
        String filePath = "documents/" + userId;
        if (leaveRequestId != null) {
            filePath += "/leave-requests/" + leaveRequestId;
        }

        FileUploadResultDTO uploadResult = fileStorageService.uploadFile(file, filePath);

        // Create document entry in database
        Document document = Document.builder()
                .filename(file.getOriginalFilename())
                .fileUrl(uploadResult.getFileUrl())
                .fileType(file.getContentType())
                .s3Key(uploadResult.getS3Key())
                .user(user)
                .leaveRequest(leaveRequest)
                .build();

        Document savedDocument = documentRepository.save(document);
        logger.info("Document uploaded successfully with ID: {}", savedDocument.getId());

        return convertToDocumentDTO(savedDocument);
    }

    /**
     * Convert Document entity to DTO
     */
    private DocumentDTO convertToDocumentDTO(Document document) {
        return DocumentDTO.builder()
                .id(document.getId())
                .filename(document.getFilename())
                .fileUrl(document.getFileUrl())
                .fileType(document.getFileType())
                .createdAt(document.getCreatedAt())
                .build();
    }
}