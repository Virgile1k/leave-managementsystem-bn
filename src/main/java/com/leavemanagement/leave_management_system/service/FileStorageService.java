package com.leavemanagement.leave_management_system.service;

import lombok.RequiredArgsConstructor;
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

    @Value("${AWS_S3_BUCKET_NAME:capitalistlogistics}")
    private String bucketName;

    // Instead of hardcoding the URL, construct it from region and bucket name
    @Value("${AWS_REGION:eu-north-1}")
    private String region;

    public String uploadProfileImage(MultipartFile file, UUID userId) throws IOException {
        String extension = getFileExtension(file.getOriginalFilename());
        String key = "profile-images/" + userId + "-" + UUID.randomUUID() + extension;

        PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                .bucket(bucketName)
                .key(key)
                .contentType(file.getContentType())
                .build();

        s3Client.putObject(putObjectRequest, RequestBody.fromBytes(file.getBytes()));

        // Construct the URL based on the AWS region and bucket name
        return "https://" + bucketName + ".s3." + region + ".amazonaws.com/" + key;
    }

    private String getFileExtension(String filename) {
        if (filename == null || filename.lastIndexOf(".") == -1) {
            return ".jpg"; // default extension
        }
        return filename.substring(filename.lastIndexOf("."));
    }
}