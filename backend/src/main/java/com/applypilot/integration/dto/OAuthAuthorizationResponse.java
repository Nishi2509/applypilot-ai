package com.applypilot.integration.dto;
import java.time.Instant;
public record OAuthAuthorizationResponse(String authorizationUrl, Instant expiresAt) { }
