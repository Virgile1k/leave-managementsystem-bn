package com.leavemanagement.leave_management_system.security;

import com.leavemanagement.leave_management_system.dto.AuthResponse;
import com.leavemanagement.leave_management_system.dto.MicrosoftAuthRequest;
import com.leavemanagement.leave_management_system.enums.UserRole;
import com.leavemanagement.leave_management_system.model.User;
import com.leavemanagement.leave_management_system.repository.UserRepository;
import com.leavemanagement.leave_management_system.util.JwtTokenUtil;
import com.microsoft.aad.msal4j.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.stereotype.Service;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

@Service
public class MicrosoftAuthService {

    @Value("${ms.auth.client-id}")
    private String clientId;

    @Value("${ms.auth.client-secret}")
    private String clientSecret;

    @Value("${ms.auth.tenant-id}")
    private String tenantId;

    @Value("${ms.auth.redirect-uri}")
    private String redirectUri;

    private final UserRepository userRepository;
    private final UserDetailsService userDetailsService;
    private final JwtTokenUtil jwtTokenUtil;

    // Constructor with all three required dependencies
    public MicrosoftAuthService(UserRepository userRepository, UserDetailsService userDetailsService, JwtTokenUtil jwtTokenUtil) {
        this.userRepository = userRepository;
        this.userDetailsService = userDetailsService;
        this.jwtTokenUtil = jwtTokenUtil;
    }

    public AuthResponse authenticateWithMicrosoft(MicrosoftAuthRequest authRequest) {
        try {
            // Build the MSAL client - Fixed builder parameters
            ConfidentialClientApplication app = ConfidentialClientApplication.builder(
                            clientId,
                            ClientCredentialFactory.createFromSecret(clientSecret))
                    .authority(String.format("https://login.microsoftonline.com/%s/", tenantId))
                    .build();

            // Define the scope
            Set<String> scopes = new HashSet<>();
            scopes.add("https://graph.microsoft.com/User.Read");

            // Build the auth code parameters
            AuthorizationCodeParameters parameters = AuthorizationCodeParameters.builder(
                            authRequest.getCode(),
                            new java.net.URI(redirectUri))
                    .scopes(scopes)
                    .build();

            // Exchange the auth code for tokens
            CompletableFuture<IAuthenticationResult> future = app.acquireToken(parameters);
            IAuthenticationResult result = future.get();

            // Extract user info from ID token claims
            String email = result.account().username();

            // Extract name from email (temporary solution)
            String fullName = email.split("@")[0];  // Use a better method if available from Microsoft Graph

            // Domain restriction check for @ist.com emails
            if (!email.toLowerCase().endsWith("@ist.com")) {
                throw new RuntimeException("Login restricted to @ist.com email addresses");
            }

            // Find or create user in database
            Optional<User> existingUser = userRepository.findByEmail(email);
            User user;

            if (existingUser.isPresent()) {
                user = existingUser.get();
            } else {
                // Create new user with default role using builder pattern
                user = User.builder()
                        .email(email)
                        .fullName(fullName)  // Setting required fullName field
                        .role(UserRole.valueOf("STAFF"))  // Default role
                        .build();

                userRepository.save(user);
            }

            // Create Spring authentication
            UserDetails userDetails = userDetailsService.loadUserByUsername(email);
            UsernamePasswordAuthenticationToken authentication =
                    new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
            SecurityContextHolder.getContext().setAuthentication(authentication);

            // Generate JWT and refresh tokens
            String jwtToken = jwtTokenUtil.generateToken(authentication);
            String refreshToken = jwtTokenUtil.generateRefreshToken(authentication);

            // Return tokens and user info as AuthResponse instead of MicrosoftAuthResponse
            return AuthResponse.builder()
                    .token(jwtToken)
                    .refreshToken(refreshToken)
                    .email(email)
                    .role(String.valueOf(user.getRole()))
                    .fullName(user.getFullName())
                    .build();

        } catch (MalformedURLException | InterruptedException | ExecutionException e) {
            throw new RuntimeException("Microsoft authentication failed: " + e.getMessage(), e);
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }
}