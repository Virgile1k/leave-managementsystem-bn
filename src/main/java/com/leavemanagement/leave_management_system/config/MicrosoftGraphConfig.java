//package com.leavemanagement.leave_management_system.config;
//
//import com.azure.identity.ClientSecretCredential;
//import com.azure.identity.ClientSecretCredentialBuilder;
//import com.microsoft.graph.authentication.TokenCredentialAuthProvider;
//import com.microsoft.graph.requests.GraphServiceClient;
//import lombok.extern.slf4j.Slf4j;
//import org.springframework.beans.factory.annotation.Value;
//import org.springframework.context.annotation.Bean;
//import org.springframework.context.annotation.Configuration;
//
//import java.util.Arrays;
//import java.util.List;
//
//@Configuration
//@Slf4j
//public class MicrosoftGraphConfig {
//
//    @Value("${azure.tenant-id}")
//    private String tenantId;
//
//    @Value("${azure.client-id}")
//    private String clientId;
//
//    @Value("${azure.client-secret}")
//    private String clientSecret;
//
//    @Value("${azure.authority:https://login.microsoftonline.com/common}")
//    private String authority;
//
//    @Value("${outlook.calendar.enabled:false}")
//    private boolean outlookCalendarEnabled;
//
//    @Bean
//    public GraphServiceClient graphServiceClient() {
//        if (!outlookCalendarEnabled) {
//            log.info("Outlook calendar integration is disabled. Not configuring GraphServiceClient.");
//            return null;
//        }
//
//        try {
//            log.info("Configuring Microsoft Graph client with tenant ID: {}", tenantId);
//
//            // Define the scopes required for Microsoft Graph
//            final List<String> scopes = Arrays.asList("https://graph.microsoft.com/.default");
//
//            // Build the client credential for Azure AD authentication
//            final ClientSecretCredential clientSecretCredential = new ClientSecretCredentialBuilder()
//                    .clientId(clientId)
//                    .clientSecret(clientSecret)
//                    .tenantId(tenantId)
//                    .authorityHost(authority)
//                    .build();
//
//            // Create the auth provider using the client credential
//            final TokenCredentialAuthProvider tokenCredentialAuthProvider =
//                    new TokenCredentialAuthProvider(scopes, clientSecretCredential);
//
//            // Build and return the Graph client
//            return GraphServiceClient
//                    .builder()
//                    .authenticationProvider(tokenCredentialAuthProvider)
//                    .buildClient();
//        } catch (Exception e) {
//            log.error("Failed to configure Microsoft Graph client", e);
//            return null;
//        }
//    }
//}