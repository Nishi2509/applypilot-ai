package com.applypilot.integration.entity;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity @Table(name = "oauth_authorization_states")
public class OAuthAuthorizationState {
    @Id @GeneratedValue(strategy = GenerationType.UUID) private UUID id;
    @Column(name = "user_id", nullable = false) private UUID userId;
    @Column(name = "state_hash", nullable = false, unique = true, length = 64, columnDefinition = "CHAR(64)") private String stateHash;
    @Column(nullable = false, length = 50) private String provider;
    @Column(name = "requested_scopes", nullable = false, length = 2000) private String requestedScopes;
    @Column(name = "expires_at", nullable = false) private Instant expiresAt;
    @Column(name = "created_at", nullable = false, updatable = false) private Instant createdAt;
    @PrePersist void created() { createdAt = Instant.now(); }
    public UUID getUserId() { return userId; }
    public String getRequestedScopes() { return requestedScopes; }
    public Instant getExpiresAt() { return expiresAt; }
    public void create(UUID userId, String stateHash, String provider, String requestedScopes, Instant expiresAt) { this.userId = userId; this.stateHash = stateHash; this.provider = provider; this.requestedScopes = requestedScopes; this.expiresAt = expiresAt; }
}
