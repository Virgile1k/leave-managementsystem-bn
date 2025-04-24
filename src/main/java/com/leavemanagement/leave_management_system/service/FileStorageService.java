package com.leavemanagement.leave_management_system.service;

import com.leavemanagement.leave_management_system.dto.FileUploadResultDTO;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.io.IOException;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class FileStorageService {

    private final S3Client s3Client;
    private static final Logger logger = LoggerFactory.getLogger(FileStorageService.class);

    @Value("${AWS_S3_BUCKET_NAME:capitalistlogistics}")
    private String bucketName;

    @Value("${AWS_REGION:eu-north-1}")
    private String region;

    /**
     * Uploads a profile image to S3
     */
    public FileUploadResultDTO uploadProfileImage(MultipartFile file, UUID userId) throws IOException {
        String extension = getFileExtension(file.getOriginalFilename());
        String key = "profile-images/" + userId + "-" + UUID.randomUUID() + extension;

        return uploadToS3(file, key);
    }

    /**
     * Uploads any file to S3 with a specified path
     */
    public FileUploadResultDTO uploadFile(MultipartFile file, String basePath) throws IOException {
        String filename = sanitizeFilename(file.getOriginalFilename());
        String uniqueId = UUID.randomUUID().toString().substring(0, 8);
        String key = basePath + "/" + uniqueId + "-" + filename;

        return uploadToS3(file, key);
    }

    /**
     * Common method to handle S3 uploads
     */
    private FileUploadResultDTO uploadToS3(MultipartFile file, String key) throws IOException {
        logger.info("Uploading file to S3: {}", key);

        PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                .bucket(bucketName)
                .key(key)
                .contentType(file.getContentType())
                .build();

        s3Client.putObject(putObjectRequest, RequestBody.fromBytes(file.getBytes()));

        // Construct the URL based on the AWS region and bucket name
        String fileUrl = "https://" + bucketName + ".s3." + region + ".amazonaws.com/" + key;
        logger.info("File uploaded successfully. URL: {}", fileUrl);

        return FileUploadResultDTO.builder()
                .fileUrl(fileUrl)
                .s3Key(key)
                .build();
    }

    /**
     * Gets the file extension from a filename
     */
    private String getFileExtension(String filename) {
        if (filename == null || filename.lastIndexOf(".") == -1) {
            return ".bin"; // default extension for binary files
        }
        return filename.substring(filename.lastIndexOf("."));
    }

    /**
     * Sanitizes a filename to prevent path traversal and other security issues
     */
    private String sanitizeFilename(String filename) {
        if (filename == null) {
            return "unnamed-file";
        }

        // Remove path traversal characters and limit length
        String sanitized = filename.replaceAll("[^a-zA-Z0-9._-]", "_");

        // Limit filename length
        if (sanitized.length() > 100) {
            String extension = getFileExtension(sanitized);
            sanitized = sanitized.substring(0, 96) + extension;
        }

        return sanitized;
    }
}