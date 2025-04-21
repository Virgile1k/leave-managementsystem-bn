package com.leavemanagement.leave_management_system.security;

import com.leavemanagement.leave_management_system.dto.AuthResponse;
import com.leavemanagement.leave_management_system.dto.MicrosoftAuthRequest;
import com.leavemanagement.leave_management_system.enums.UserRole;
import com.leavemanagement.leave_management_system.model.User;
import com.leavemanagement.leave_management_system.repository.UserRepository;
import com.leavemanagement.leave_management_system.util.JwtTokenUtil;
import com.microsoft.aad.msal4j.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.stereotype.Service;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

@Service
public class MicrosoftAuthService {
    private static final Logger logger = LoggerFactory.getLogger(MicrosoftAuthService.class);

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

    public MicrosoftAuthService(UserRepository userRepository, UserDetailsService userDetailsService, JwtTokenUtil jwtTokenUtil) {
        this.userRepository = userRepository;
        this.userDetailsService = userDetailsService;
        this.jwtTokenUtil = jwtTokenUtil;
    }

    /**
     * Authenticate a user using Microsoft OAuth authorization code
     * @param authRequest Request containing the authorization code
     * @return AuthResponse containing JWT token and user details
     */
    public AuthResponse authenticateWithMicrosoft(MicrosoftAuthRequest authRequest) {
        logger.debug("Starting Microsoft authentication process");

        try {
            // Validate the request
            if (authRequest.getCode() == null || authRequest.getCode().isEmpty()) {
                logger.error("Microsoft authentication failed: Authorization code is missing");
                throw new RuntimeException("Authorization code is required");
            }

            // Build the MSAL confidential client application
            logger.debug("Building MSAL client application");
            ConfidentialClientApplication app = ConfidentialClientApplication.builder(
                            clientId,
                            ClientCredentialFactory.createFromSecret(clientSecret))
                    .authority(String.format("https://login.microsoftonline.com/%s/", tenantId))
                    .build();

            // Define the scopes needed for Microsoft Graph API
            Set<String> scopes = new HashSet<>();
            scopes.add("https://graph.microsoft.com/User.Read");

            // Exchange the authorization code for tokens
            logger.debug("Exchanging authorization code for tokens");
            AuthorizationCodeParameters parameters = AuthorizationCodeParameters.builder(
                            authRequest.getCode(),
                            new URI(redirectUri))
                    .scopes(scopes)
                    .build();

            // Acquire token by authorization code
            CompletableFuture<IAuthenticationResult> future = app.acquireToken(parameters);
            IAuthenticationResult result = future.get();

            // Extract user information from the account
            String email = result.account().username();
            logger.info("Successfully authenticated Microsoft user: {}", email);

            // Extract additional claims from the ID token
            String fullName = result.account().username().split("@")[0];
            // Note: In a production environment, you would use Microsoft Graph API to get more user details

            // Comment out the domain restriction for now
            // This will be re-enabled in production
            /*
            if (!email.toLowerCase().endsWith("@ist.com")) {
                logger.warn("Login attempt from unauthorized domain: {}", email);
                throw new RuntimeException("Login restricted to @ist.com email addresses");
            }
            */
            // Log that we're allowing all email domains in development
            logger.info("Email domain restriction bypassed in development for: {}", email);

            // Find or create user in the database
            Optional<User> existingUser = userRepository.findByEmail(email);
            User user;

            if (existingUser.isPresent()) {
                logger.debug("Found existing user: {}", email);
                user = existingUser.get();
                // Optionally update user data here if needed
            } else {
                logger.info("Creating new user from Microsoft authentication: {}", email);
                // Create new user with default role
                user = User.builder()
                        .email(email)
                        .fullName(fullName)
                        .role(UserRole.STAFF) // Default role for new users
                        .build();
                userRepository.save(user);
            }

            // Create authentication for Spring Security context
            UserDetails userDetails = userDetailsService.loadUserByUsername(email);
            UsernamePasswordAuthenticationToken authentication =
                    new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
            SecurityContextHolder.getContext().setAuthentication(authentication);

            // Generate JWT and refresh tokens
            logger.debug("Generating JWT and refresh tokens");
            String jwtToken = jwtTokenUtil.generateToken(authentication);
            String refreshToken = jwtTokenUtil.generateRefreshToken(authentication);

            // Return authentication response
            return AuthResponse.builder()
                    .token(jwtToken)
                    .refreshToken(refreshToken)
                    .email(email)
                    .role(user.getRole().name())
                    .fullName(user.getFullName())
                    .build();

        } catch (MalformedURLException | URISyntaxException e) {
            logger.error("Microsoft authentication failed: Invalid URL", e);
            throw new RuntimeException("Microsoft authentication failed: Invalid URL configuration", e);
        } catch (InterruptedException e) {
            logger.error("Microsoft authentication interrupted", e);
            Thread.currentThread().interrupt();
            throw new RuntimeException("Microsoft authentication was interrupted", e);
        } catch (ExecutionException e) {
            logger.error("Microsoft authentication execution failed", e);
            throw new RuntimeException("Microsoft authentication failed: " + e.getCause().getMessage(), e);
        } catch (Exception e) {
            logger.error("Unexpected error during Microsoft authentication", e);
            throw new RuntimeException("Microsoft authentication failed: " + e.getMessage(), e);
        }
    }

    /**
     * Generate the Microsoft OAuth authorization URL
     * @return URL to redirect the user to for Microsoft authentication
     */
    public String generateAuthorizationUrl() {
        return String.format(
                "https://login.microsoftonline.com/%s/oauth2/v2.0/authorize?client_id=%s&response_type=code&redirect_uri=%s&response_mode=query&scope=%s",
                tenantId,
                clientId,
                redirectUri,
                "openid%20profile%20email%20offline_access"
        );
    }
}