package com.leavemanagement.leave_management_system.service;

import com.leavemanagement.leave_management_system.dto.*;
import com.leavemanagement.leave_management_system.enums.UserRole;
import com.leavemanagement.leave_management_system.exceptions.ResourceNotFoundException;
import com.leavemanagement.leave_management_system.exceptions.UserAlreadyExistsException;
import com.leavemanagement.leave_management_system.model.Department;
import com.leavemanagement.leave_management_system.model.User;
import com.leavemanagement.leave_management_system.repository.DepartmentRepository;
import com.leavemanagement.leave_management_system.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final DepartmentRepository departmentRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public UserResponseDto createUser(UserRequestDto userRequestDto) {
        // Check if user already exists with the given email
        if (userRepository.existsByEmail(userRequestDto.getEmail())) {
            throw new UserAlreadyExistsException("User with email " + userRequestDto.getEmail() + " already exists");
        }

        User user = new User();
        user.setEmail(userRequestDto.getEmail());
        user.setFullName(userRequestDto.getFullName());
        user.setPassword(passwordEncoder.encode(userRequestDto.getPassword()));
        user.setRole(userRequestDto.getRole());
        user.setProfilePicUrl(userRequestDto.getProfilePicUrl());

        // Set manager if managerId is provided
        if (userRequestDto.getManagerId() != null) {
            User manager = userRepository.findById(userRequestDto.getManagerId())
                    .orElseThrow(() -> new ResourceNotFoundException("Manager not found with id: " + userRequestDto.getManagerId()));
            user.setManager(manager);
        }

        // Set department if departmentId is provided
        if (userRequestDto.getDepartmentId() != null) {
            Department department = departmentRepository.findById(userRequestDto.getDepartmentId())
                    .orElseThrow(() -> new ResourceNotFoundException("Department not found with id: " + userRequestDto.getDepartmentId()));
            user.setDepartment(department);
        }

        User savedUser = userRepository.save(user);
        return mapUserToUserResponseDto(savedUser);
    }

    @Override
    @Transactional(readOnly = true)
    public UserResponseDto getUserById(UUID id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + id));
        return mapUserToUserResponseDto(user);
    }

    @Override
    @Transactional(readOnly = true)
    public UserResponseDto getUserByEmail(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with email: " + email));
        return mapUserToUserResponseDto(user);
    }

    @Override
    @Transactional
    public UserResponseDto updateUser(UUID id, UserUpdateDto userUpdateDto) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + id));

        if (userUpdateDto.getFullName() != null) {
            user.setFullName(userUpdateDto.getFullName());
        }

        if (userUpdateDto.getPassword() != null) {
            user.setPassword(passwordEncoder.encode(userUpdateDto.getPassword()));
        }

        if (userUpdateDto.getRole() != null) {
            user.setRole(userUpdateDto.getRole());
        }

        if (userUpdateDto.getProfilePicUrl() != null) {
            user.setProfilePicUrl(userUpdateDto.getProfilePicUrl());
        }

        // Update manager if managerId is provided
        if (userUpdateDto.getManagerId() != null) {
            if (userUpdateDto.getManagerId().equals(id)) {
                throw new IllegalArgumentException("User cannot be their own manager");
            }

            User manager = userRepository.findById(userUpdateDto.getManagerId())
                    .orElseThrow(() -> new ResourceNotFoundException("Manager not found with id: " + userUpdateDto.getManagerId()));
            user.setManager(manager);
        }

        // Update department if departmentId is provided
        if (userUpdateDto.getDepartmentId() != null) {
            Department department = departmentRepository.findById(userUpdateDto.getDepartmentId())
                    .orElseThrow(() -> new ResourceNotFoundException("Department not found with id: " + userUpdateDto.getDepartmentId()));
            user.setDepartment(department);
        }

        User updatedUser = userRepository.save(user);
        return mapUserToUserResponseDto(updatedUser);
    }

    @Override
    @Transactional
    public void deleteUser(UUID id) {
        if (!userRepository.existsById(id)) {
            throw new ResourceNotFoundException("User not found with id: " + id);
        }
        userRepository.deleteById(id);
    }

    @Override
    @Transactional
    public UserResponseDto changePassword(UUID userId, String currentPassword, String newPassword) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));

        // Verify current password
        if (!passwordEncoder.matches(currentPassword, user.getPassword())) {
            throw new IllegalArgumentException("Current password is incorrect");
        }

        // Update password
        user.setPassword(passwordEncoder.encode(newPassword));
        User updatedUser = userRepository.save(user);

        return mapUserToUserResponseDto(updatedUser);
    }

    @Override
    @Transactional(readOnly = true)
    public List<UserResponseDto> getAllUsers() {
        return userRepository.findAll().stream()
                .map(this::mapUserToUserResponseDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<UserResponseDto> getUsersByManager(UUID managerId) {
        return userRepository.findByManagerId(managerId).stream()
                .map(this::mapUserToUserResponseDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<UserResponseDto> getUsersByDepartment(UUID departmentId) {
        return userRepository.findByDepartmentId(departmentId).stream()
                .map(this::mapUserToUserResponseDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<UserResponseDto> getUsersByRole(String role) {
        try {
            UserRole userRole = UserRole.valueOf(role.toUpperCase());
            return userRepository.findByRole(userRole.name()).stream()
                    .map(this::mapUserToUserResponseDto)
                    .collect(Collectors.toList());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid role: " + role);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<UserResponseDto> searchUsers(String searchTerm) {
        return userRepository.searchUsers(searchTerm).stream()
                .map(this::mapUserToUserResponseDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<UserSummaryDto> getAllManagers() {
        return userRepository.findAllManagers().stream()
                .map(this::mapUserToUserSummaryDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<User> getHRAdmins() {
        return userRepository.findByRole(UserRole.ADMIN.name());
    }

    private UserResponseDto mapUserToUserResponseDto(User user) {
        UserResponseDto responseDto = new UserResponseDto();
        responseDto.setId(user.getId());
        responseDto.setEmail(user.getEmail());
        responseDto.setFullName(user.getFullName());
        responseDto.setRole(user.getRole());
        responseDto.setProfilePicUrl(user.getProfilePicUrl());

        // Map manager if exists
        if (user.getManager() != null) {
            responseDto.setManager(mapUserToUserSummaryDto(user.getManager()));
        }

        // Map department if exists
        if (user.getDepartment() != null) {
            responseDto.setDepartmentId(user.getDepartment().getId());
            responseDto.setDepartmentName(user.getDepartment().getName());
        }

        return responseDto;
    }

    private UserSummaryDto mapUserToUserSummaryDto(User user) {
        return UserSummaryDto.builder()
                .id(user.getId())
                .email(user.getEmail())
                .fullName(user.getFullName())
                .role(user.getRole())
                .build();
    }
}