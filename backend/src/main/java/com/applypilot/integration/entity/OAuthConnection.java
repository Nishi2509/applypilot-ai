package com.applypilot.integration.entity;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "oauth_connections", uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "provider"}))
public class OAuthConnection {
    @Id @GeneratedValue(strategy = GenerationType.UUID) private UUID id;
    @Column(name = "user_id", nullable = false) private UUID userId;
    @Column(nullable = false, length = 50) private String provider;
    @Lob @Column(name = "encrypted_access_token", nullable = false, columnDefinition = "MEDIUMTEXT") private String encryptedAccessToken;
    @Lob @Column(name = "encrypted_refresh_token", columnDefinition = "MEDIUMTEXT") private String encryptedRefreshToken;
    @Column(name = "expires_at") private Instant expiresAt;
    @Column(length = 2000) private String scopes;
    @Column(name = "created_at", nullable = false, updatable = false) private Instant createdAt;
    @Column(name = "updated_at", nullable = false) private Instant updatedAt;
    @PrePersist void created() { createdAt = updatedAt = Instant.now(); }
    @PreUpdate void updated() { updatedAt = Instant.now(); }
    public UUID getUserId() { return userId; }
    public String getProvider() { return provider; }
    public String getEncryptedAccessToken() { return encryptedAccessToken; }
    public String getEncryptedRefreshToken() { return encryptedRefreshToken; }
    public Instant getExpiresAt() { return expiresAt; }
    public String getScopes() { return scopes; }
    public void create(UUID userId, String provider, String accessToken, String refreshToken, Instant expiresAt, String scopes) { this.userId = userId; this.provider = provider; update(accessToken, refreshToken, expiresAt, scopes); }
    public void update(String accessToken, String refreshToken, Instant expiresAt, String scopes) { encryptedAccessToken = accessToken; if (refreshToken != null) encryptedRefreshToken = refreshToken; this.expiresAt = expiresAt; this.scopes = scopes; }
}
