package com.leavemanagement.leave_management_system.service;

import com.leavemanagement.leave_management_system.dto.AuthResponse;
import com.leavemanagement.leave_management_system.dto.LoginRequest;
import com.leavemanagement.leave_management_system.dto.MicrosoftAuthRequest;
import com.leavemanagement.leave_management_system.exceptions.InvalidCredentialsException;
import com.leavemanagement.leave_management_system.exceptions.TokenException;
import com.leavemanagement.leave_management_system.model.User;
import com.leavemanagement.leave_management_system.repository.UserRepository;
import com.leavemanagement.leave_management_system.security.MicrosoftAuthService;
import com.leavemanagement.leave_management_system.util.JwtTokenUtil;
import io.jsonwebtoken.ExpiredJwtException;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.stereotype.Service;

@Service
public class AuthServiceImpl implements AuthService {

    private final AuthenticationManager authenticationManager;
    private final UserDetailsService userDetailsService;
    private final JwtTokenUtil jwtTokenUtil;
    private final UserRepository userRepository;
    private final MicrosoftAuthService microsoftAuthService;

    public AuthServiceImpl(AuthenticationManager authenticationManager,
                           UserDetailsService userDetailsService,
                           JwtTokenUtil jwtTokenUtil,
                           UserRepository userRepository,
                           MicrosoftAuthService microsoftAuthService) {
        this.authenticationManager = authenticationManager;
        this.userDetailsService = userDetailsService;
        this.jwtTokenUtil = jwtTokenUtil;
        this.userRepository = userRepository;
        this.microsoftAuthService = microsoftAuthService;
    }

    @Override
    public AuthResponse login(LoginRequest loginRequest) {
        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(loginRequest.getEmail(), loginRequest.getPassword())
            );

            SecurityContextHolder.getContext().setAuthentication(authentication);

            final UserDetails userDetails = (UserDetails) authentication.getPrincipal();
            final String token = jwtTokenUtil.generateToken(authentication);
            final String refreshToken = jwtTokenUtil.generateRefreshToken(authentication);

            User user = userRepository.findByEmail(userDetails.getUsername())
                    .orElseThrow(() -> new InvalidCredentialsException("User not found"));

            return buildAuthResponse(user, token, refreshToken);
        } catch (BadCredentialsException e) {
            throw new InvalidCredentialsException("Invalid email or password");
        }
    }

    @Override
    public AuthResponse authenticateWithMicrosoft(MicrosoftAuthRequest authRequest) {
        // Call microsoftAuthService and return the AuthResponse directly
        // This fixes the incompatible type issue
        return microsoftAuthService.authenticateWithMicrosoft(authRequest);
    }

    @Override
    public AuthResponse refreshToken(String refreshToken) {
        try {
            String email = jwtTokenUtil.getUsernameFromToken(refreshToken);

            // Validate if it's a refresh token
            if (!"refresh".equals(jwtTokenUtil.getClaimFromToken(refreshToken, claims -> claims.get("type")))) {
                throw new TokenException("Invalid refresh token");
            }

            // Check if token is expired
            if (jwtTokenUtil.isTokenExpired(refreshToken)) {
                throw new TokenException("Refresh token has expired");
            }

            UserDetails userDetails = userDetailsService.loadUserByUsername(email);

            UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                    userDetails, null, userDetails.getAuthorities());

            String newToken = jwtTokenUtil.generateToken(authentication);
            String newRefreshToken = jwtTokenUtil.generateRefreshToken(authentication);

            User user = userRepository.findByEmail(email)
                    .orElseThrow(() -> new TokenException("User not found"));

            return buildAuthResponse(user, newToken, newRefreshToken);
        } catch (ExpiredJwtException e) {
            throw new TokenException("Refresh token has expired");
        } catch (Exception e) {
            throw new TokenException("Invalid refresh token");
        }
    }

    @Override
    public AuthResponse getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        UserDetails userDetails = (UserDetails) authentication.getPrincipal();

        User user = userRepository.findByEmail(userDetails.getUsername())
                .orElseThrow(() -> new InvalidCredentialsException("User not found"));

        String token = jwtTokenUtil.generateToken(authentication);
        String refreshToken = jwtTokenUtil.generateRefreshToken(authentication);

        return buildAuthResponse(user, token, refreshToken);
    }

    private AuthResponse buildAuthResponse(User user, String token, String refreshToken) {
        return AuthResponse.builder()
                .token(token)
                .refreshToken(refreshToken)
                .email(user.getEmail())
                .fullName(user.getFullName())
                .role(user.getRole().name())
                .build();
    }
}