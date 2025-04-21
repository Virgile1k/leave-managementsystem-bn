package com.leavemanagement.leave_management_system.config;

import com.azure.identity.ClientSecretCredential;
import com.azure.identity.ClientSecretCredentialBuilder;
import com.microsoft.graph.authentication.TokenCredentialAuthProvider;
import com.microsoft.graph.requests.GraphServiceClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Arrays;
import java.util.List;

@Configuration
public class MicrosoftGraphConfig {

    @Value("${azure.tenant-id:}")
    private String tenantId;

    @Value("${azure.client-id:}")
    private String clientId;

    @Value("${azure.client-secret:}")
    private String clientSecret;

    @Value("${outlook.calendar.enabled:false}")
    private boolean outlookCalendarEnabled;

    private static final List<String> GRAPH_SCOPES = Arrays.asList("https://graph.microsoft.com/.default");

    @Bean
    public GraphServiceClient graphServiceClient() {
        if (!outlookCalendarEnabled) {
            return null;
        }

        final ClientSecretCredential credential = new ClientSecretCredentialBuilder()
                .tenantId(tenantId)
                .clientId(clientId)
                .clientSecret(clientSecret)
                .build();

        final TokenCredentialAuthProvider authProvider = new TokenCredentialAuthProvider(GRAPH_SCOPES, credential);

        return GraphServiceClient.builder()
                .authenticationProvider(authProvider)
                .buildClient();
    }
}