package com.leavemanagement.leave_management_system.dto;

import com.leavemanagement.leave_management_system.enums.UserRole;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserResponseDto {

    private UUID id;

    private String email;

    private String fullName;

    private UserRole role;

    private UserSummaryDto manager;

    private UUID departmentId;

    private String departmentName;

    private String profilePicUrl;
}
