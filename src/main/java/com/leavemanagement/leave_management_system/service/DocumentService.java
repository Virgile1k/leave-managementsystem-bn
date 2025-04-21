package com.leavemanagement.leave_management_system.service;

import com.leavemanagement.leave_management_system.dto.DocumentDTO;
import com.leavemanagement.leave_management_system.exceptions.ResourceNotFoundException;
import com.leavemanagement.leave_management_system.model.Document;
import com.leavemanagement.leave_management_system.model.LeaveRequest;
import com.leavemanagement.leave_management_system.model.User;
import com.leavemanagement.leave_management_system.repository.DocumentRepository;
import com.leavemanagement.leave_management_system.repository.LeaveRequestRepository;
import com.leavemanagement.leave_management_system.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.io.IOException;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class DocumentService {

    private final S3Client s3Client;
    private final DocumentRepository documentRepository;
    private final UserRepository userRepository;
    private final LeaveRequestRepository leaveRequestRepository;

    @Value("${AWS_S3_BUCKET_NAME:capitalistlogistics}")
    private String bucketName;

    @Value("${AWS_REGION:eu-north-1}")
    private String region;

    @Transactional
    public DocumentDTO uploadDocument(UUID userId, MultipartFile file, UUID leaveRequestId) throws IOException {
        // Get user entity
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        // Check if leave request exists and belongs to the user
        LeaveRequest leaveRequest = null;
        if (leaveRequestId != null) {
            leaveRequest = leaveRequestRepository.findById(leaveRequestId)
                    .orElseThrow(() -> new ResourceNotFoundException("Leave request not found"));

            if (!leaveRequest.getUser().getId().equals(userId)) {
                throw new IllegalArgumentException("Leave request doesn't belong to the user");
            }
        }

        // Generate a unique key for S3
        String fileExtension = getFileExtension(file.getOriginalFilename());
        String key = "leave-documents/" + userId + "/" + UUID.randomUUID() + fileExtension;

        // Upload file to S3
        PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                .bucket(bucketName)
                .key(key)
                .contentType(file.getContentType())
                .build();

        s3Client.putObject(putObjectRequest, RequestBody.fromBytes(file.getBytes()));

        // Construct the file URL
        String fileUrl = "https://" + bucketName + ".s3." + region + ".amazonaws.com/" + key;

        // Save document information in the database
        Document document = Document.builder()
                .user(user)
                .leaveRequest(leaveRequest)
                .filename(file.getOriginalFilename())
                .fileUrl(fileUrl)
                .fileType(file.getContentType())
                .s3Key(key)
                .build();

        Document savedDocument = documentRepository.save(document);

        return convertToDocumentDTO(savedDocument);
    }

    @Transactional
    public void deleteDocument(UUID documentId, UUID userId) {
        Document document = documentRepository.findById(documentId)
                .orElseThrow(() -> new ResourceNotFoundException("Document not found"));

        // Validate that the document belongs to the user
        if (!document.getUser().getId().equals(userId)) {
            throw new IllegalArgumentException("Document doesn't belong to the user");
        }

        // Delete from S3
        DeleteObjectRequest deleteObjectRequest = DeleteObjectRequest.builder()
                .bucket(bucketName)
                .key(document.getS3Key())
                .build();

        s3Client.deleteObject(deleteObjectRequest);

        // Delete from database
        documentRepository.delete(document);
    }

    public List<DocumentDTO> getUserDocuments(UUID userId) {
        return documentRepository.findByUserId(userId).stream()
                .map(this::convertToDocumentDTO)
                .collect(Collectors.toList());
    }

    public List<DocumentDTO> getLeaveRequestDocuments(UUID leaveRequestId) {
        return documentRepository.findByLeaveRequestId(leaveRequestId).stream()
                .map(this::convertToDocumentDTO)
                .collect(Collectors.toList());
    }

    private String getFileExtension(String filename) {
        if (filename == null || filename.lastIndexOf(".") == -1) {
            return ".pdf"; // default extension
        }
        return filename.substring(filename.lastIndexOf("."));
    }

    private DocumentDTO convertToDocumentDTO(Document document) {
        return DocumentDTO.builder()
                .id(document.getId())
                .filename(document.getFilename())
                .fileUrl(document.getFileUrl())
                .fileType(document.getFileType())
                .uploadDate(document.getCreatedAt())
                .leaveRequestId(document.getLeaveRequest() != null ? document.getLeaveRequest().getId() : null)
                .build();
    }
}