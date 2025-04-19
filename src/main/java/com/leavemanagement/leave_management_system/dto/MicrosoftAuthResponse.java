package com.leavemanagement.leave_management_system.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response DTO for Microsoft authentication
 * Contains tokens and user information returned after successful Microsoft authentication
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MicrosoftAuthResponse {
    private String token;           // JWT access token
    private String refreshToken;    // JWT refresh token
    private String email;           // User's email address
    private String role;            // User's role in the system
    private String fullName;        // User's full name from Microsoft account
    private String profilePicUrl;   // URL to the user's profile picture (optional)
}