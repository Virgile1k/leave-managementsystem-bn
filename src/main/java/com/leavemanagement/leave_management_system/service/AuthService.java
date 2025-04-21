package com.leavemanagement.leave_management_system.service;

import com.leavemanagement.leave_management_system.dto.AuthResponse;
import com.leavemanagement.leave_management_system.dto.LoginRequest;
import com.leavemanagement.leave_management_system.dto.MicrosoftAuthRequest;

public interface AuthService {
    AuthResponse login(LoginRequest loginRequest);
    AuthResponse authenticateWithMicrosoft(MicrosoftAuthRequest authRequest);
    AuthResponse refreshToken(String refreshToken);
    AuthResponse getCurrentUser();

}