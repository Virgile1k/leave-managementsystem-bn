package com.leavemanagement.leave_management_system.service;
import com.leavemanagement.leave_management_system.dto.UserRequestDto;
import com.leavemanagement.leave_management_system.dto.UserResponseDto;
import com.leavemanagement.leave_management_system.dto.UserSummaryDto;
import com.leavemanagement.leave_management_system.dto.UserUpdateDto;
import com.leavemanagement.leave_management_system.model.User;

import java.util.List;
import java.util.UUID;

public interface UserService {
    UserResponseDto createUser(UserRequestDto userRequestDto);
    UserResponseDto getUserById(UUID id);
    UserResponseDto getUserByEmail(String email);
    UserResponseDto updateUser(UUID id, UserUpdateDto userUpdateDto);
    void deleteUser(UUID id);
    List<UserResponseDto> getAllUsers();
    List<UserResponseDto> getUsersByManager(UUID managerId);
    List<UserResponseDto> getUsersByDepartment(UUID departmentId);
    List<UserResponseDto> getUsersByRole(String role);
    List<UserResponseDto> searchUsers(String searchTerm);
    List<UserSummaryDto> getAllManagers();
    List<User> getHRAdmins();
    List<UserResponseDto> getTeamMembersForCurrentUser();
    // Add to UserService interface
    UserResponseDto changePassword(UUID userId, String currentPassword, String newPassword);
}