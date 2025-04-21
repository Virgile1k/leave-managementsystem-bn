package com.leavemanagement.leave_management_system.controller;

import com.leavemanagement.leave_management_system.dto.UserResponseDto;
import com.leavemanagement.leave_management_system.dto.UserUpdateDto;
import com.leavemanagement.leave_management_system.service.FileStorageService;
import com.leavemanagement.leave_management_system.service.UserService;
import com.leavemanagement.leave_management_system.util.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import jakarta.validation.Valid;
import java.io.IOException;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/user-profile")
@RequiredArgsConstructor
public class UserProfileController {

    private final FileStorageService fileStorageService;
    private final UserService userService;
    private final SecurityUtils securityUtils;
    @GetMapping
    public ResponseEntity<UserResponseDto> getCurrentUserProfile() {
        UUID currentUserId = securityUtils.getCurrentUserId();
        return ResponseEntity.ok(userService.getUserById(currentUserId));
    }

    @PutMapping
    public ResponseEntity<UserResponseDto> updateUserProfile(
            @Valid @RequestBody UserUpdateDto userUpdateDto) {
        UUID currentUserId = securityUtils.getCurrentUserId();

        // Prevent updating sensitive fields if present in the DTO
        userUpdateDto.setRole(null);  // Role cannot be changed through profile update

        return ResponseEntity.ok(userService.updateUser(currentUserId, userUpdateDto));
    }
    @PostMapping("/image")
    public ResponseEntity<UserResponseDto> uploadProfileImage(
            @RequestParam("file") MultipartFile file) throws IOException {

        // Get the current authenticated user
        UUID currentUserId = securityUtils.getCurrentUserId();

        // Check file type
        if (!file.getContentType().startsWith("image/")) {
            throw new IllegalArgumentException("Only image files are allowed");
        }

        // Upload file to S3
        String imageUrl = fileStorageService.uploadProfileImage(file, currentUserId);

        // Update user profile with new image URL
        UserUpdateDto updateDto = new UserUpdateDto();
        updateDto.setProfilePicUrl(imageUrl);

        // Update user and return updated user
        return ResponseEntity.ok(userService.updateUser(currentUserId, updateDto));
    }
    @PutMapping("/password")
    public ResponseEntity<UserResponseDto> changePassword(
            @RequestParam("currentPassword") String currentPassword,
            @RequestParam("newPassword") String newPassword) {

        UUID currentUserId = securityUtils.getCurrentUserId();

        // Validate current password and update to new password
        UserResponseDto updatedUser = userService.changePassword(currentUserId, currentPassword, newPassword);

        return ResponseEntity.ok(updatedUser);
    }

    @DeleteMapping("/image")
    public ResponseEntity<UserResponseDto> deleteProfileImage() {
        UUID currentUserId = securityUtils.getCurrentUserId();

        // Create update DTO with null image URL
        UserUpdateDto updateDto = new UserUpdateDto();
        updateDto.setProfilePicUrl(null);

        // Update user and return updated user
        return ResponseEntity.ok(userService.updateUser(currentUserId, updateDto));
    }
}