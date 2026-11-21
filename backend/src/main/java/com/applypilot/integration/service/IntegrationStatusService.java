package com.applypilot.integration.service;

import com.applypilot.config.AppProperties;
import com.applypilot.integration.dto.IntegrationStatusResponse;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class IntegrationStatusService {
    private final GoogleOAuthService google; private final AppProperties properties;
    public IntegrationStatusService(GoogleOAuthService google, AppProperties properties) { this.google = google; this.properties = properties; }
    public IntegrationStatusResponse status(UUID userId) {
        boolean configured = google.isConfigured(); boolean connected = configured && google.isConnected(userId);
        String googleDetail = !configured ? "Google OAuth credentials or token encryption are not configured" : connected ? "Connected" : "Connection required";
        boolean aiConfigured = (properties.aiService().baseUrl() != null && !properties.aiService().baseUrl().isBlank());
        return new IntegrationStatusResponse(new IntegrationStatusResponse.IntegrationConnection(configured, connected, googleDetail), new IntegrationStatusResponse.IntegrationConnection(configured, connected, googleDetail), new IntegrationStatusResponse.IntegrationConnection(aiConfigured, false, aiConfigured ? "AI service endpoint configured" : "AI service endpoint unavailable"));
    }
}
