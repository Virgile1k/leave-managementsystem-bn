package com.leavemanagement.leave_management_system.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MicrosoftAuthRequest {
    @NotBlank(message = "Authorization code is required")
    private String code;
    // Optional redirect URI if needed for verification
    private String redirectUri;
}