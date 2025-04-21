package com.leavemanagement.leave_management_system.controller;

import com.leavemanagement.leave_management_system.dto.AuthResponse;
import com.leavemanagement.leave_management_system.dto.LoginRequest;
import com.leavemanagement.leave_management_system.dto.MicrosoftAuthRequest;
import com.leavemanagement.leave_management_system.dto.RefreshTokenRequest;
import com.leavemanagement.leave_management_system.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest loginRequest) {
        return ResponseEntity.ok(authService.login(loginRequest));
    }

    @PostMapping("/microsoft")
    public ResponseEntity<AuthResponse> microsoftLogin(@Valid @RequestBody MicrosoftAuthRequest authRequest) {
        return ResponseEntity.ok(authService.authenticateWithMicrosoft(authRequest));
    }

    @GetMapping("/microsoft/callback")
    public ResponseEntity<Void> microsoftOAuthCallback(@RequestParam("code") String code) {
        // Since we're redirecting back to the frontend, we don't need to process anything here
        // The React app will handle the code and call /api/v1/auth/microsoft
        return ResponseEntity.status(HttpStatus.FOUND)
                .header(HttpHeaders.LOCATION, "http://localhost:3000/api/v1/auth/microsoft/callback?code=" + code)
                .build();
    }

    @PostMapping("/refresh")
    public ResponseEntity<AuthResponse> refreshToken(@Valid @RequestBody RefreshTokenRequest refreshTokenRequest) {
        return ResponseEntity.ok(authService.refreshToken(refreshTokenRequest.getRefreshToken()));
    }


    @GetMapping("/me")
    public ResponseEntity<AuthResponse> getCurrentUser() {
        return ResponseEntity.ok(authService.getCurrentUser());
    }
}