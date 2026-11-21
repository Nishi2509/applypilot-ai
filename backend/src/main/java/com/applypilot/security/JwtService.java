package com.applypilot.security;

import com.applypilot.config.AppProperties;
import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.*;
import javax.crypto.SecretKey;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Service;

@Service
public class JwtService {
    private final AppProperties.Jwt config;
    public JwtService(AppProperties properties) { this.config = properties.jwt(); }
    @PostConstruct void validateConfiguredSecret() { key(); }
    private SecretKey key() { return Keys.hmacShaKeyFor(config.secret().getBytes(StandardCharsets.UTF_8)); }
    public String issue(UUID userId, String email) { Instant now = Instant.now(); return Jwts.builder().subject(userId.toString()).claim("email", email).issuedAt(Date.from(now)).expiration(Date.from(now.plusSeconds(config.expirationMinutes() * 60))).signWith(key()).compact(); }
    public UUID userId(String token) { return UUID.fromString(Jwts.parser().verifyWith(key()).build().parseSignedClaims(token).getPayload().getSubject()); }
    public long expirySeconds() { return config.expirationMinutes() * 60; }
}
