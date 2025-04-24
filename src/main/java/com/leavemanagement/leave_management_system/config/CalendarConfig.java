package com.leavemanagement.leave_management_system.config;

import com.azure.identity.ClientSecretCredential;
import com.azure.identity.ClientSecretCredentialBuilder;
import com.microsoft.graph.authentication.TokenCredentialAuthProvider;
import com.microsoft.graph.requests.GraphServiceClient;
import lombok.extern.slf4j.Slf4j;
import okhttp3.Request;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Arrays;
import java.util.List;

@Configuration
@Slf4j
public class CalendarConfig {

    @Value("${azure.tenant-id:}")
    private String tenantId;

    @Value("${azure.client-id:}")
    private String clientId;

    @Value("${azure.client-secret:}")
    private String clientSecret;

    @Bean
    @ConditionalOnProperty(name = "outlook.calendar.enabled", havingValue = "true")
    public GraphServiceClient<Request> graphServiceClient() {
        try {
            log.info("Initializing Microsoft Graph client with client credentials flow");

            // Use client credentials flow (app-only authentication)
            final ClientSecretCredential credential = new ClientSecretCredentialBuilder()
                    .clientId(clientId)
                    .clientSecret(clientSecret)
                    .tenantId(tenantId)
                    .build();

            // For client credentials, we need to use the .default scope
            List<String> scopes = Arrays.asList("https://graph.microsoft.com/.default");

            TokenCredentialAuthProvider authProvider = new TokenCredentialAuthProvider(scopes, credential);

            log.info("Microsoft Graph client initialized successfully with client credentials flow");

            return GraphServiceClient
                    .builder()
                    .authenticationProvider(authProvider)
                    .buildClient();
        } catch (Exception e) {
            log.error("Failed to initialize Microsoft Graph client: {}", e.getMessage(), e);
            throw e; // Let Spring know that bean creation failed
        }
    }
}