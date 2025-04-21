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
public class UserUpdateDto {

    private String fullName;

    private String password;

    private UserRole role;

    private UUID managerId;

    private UUID departmentId;

    private String profilePicUrl;
}

