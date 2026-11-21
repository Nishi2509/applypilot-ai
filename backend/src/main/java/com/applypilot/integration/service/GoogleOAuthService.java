package com.applypilot.integration.service;

import com.applypilot.config.AppProperties;
import com.applypilot.integration.dto.OAuthAuthorizationResponse;
import com.applypilot.integration.entity.OAuthAuthorizationState;
import com.applypilot.integration.entity.OAuthConnection;
import com.applypilot.integration.repository.OAuthAuthorizationStateRepository;
import com.applypilot.integration.repository.OAuthConnectionRepository;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.*;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Service
public class GoogleOAuthService {
    private static final String PROVIDER = "GOOGLE";
    private static final List<String> SCOPES = List.of("https://www.googleapis.com/auth/gmail.send", "https://www.googleapis.com/auth/spreadsheets");
    private final AppProperties properties; private final OAuthTokenCipher cipher; private final OAuthConnectionRepository connections; private final OAuthAuthorizationStateRepository states; private final RestClient restClient = RestClient.create(); private final SecureRandom random = new SecureRandom();
    public GoogleOAuthService(AppProperties properties, OAuthTokenCipher cipher, OAuthConnectionRepository connections, OAuthAuthorizationStateRepository states) { this.properties = properties; this.cipher = cipher; this.connections = connections; this.states = states; }
    public boolean isConfigured() { return nonBlank(properties.google().clientId()) && nonBlank(properties.google().clientSecret()) && nonBlank(properties.google().redirectUri()) && cipher.isConfigured(); }
    @Transactional public OAuthAuthorizationResponse begin(UUID userId) {
        if (!isConfigured()) throw new IllegalArgumentException("Google OAuth is not configured. Set GOOGLE_CLIENT_ID, GOOGLE_CLIENT_SECRET, GOOGLE_REDIRECT_URI, and APP_TOKEN_ENCRYPTION_KEY.");
        states.deleteByExpiresAtBefore(Instant.now()); String state = randomState(); Instant expiresAt = Instant.now().plusSeconds(600); OAuthAuthorizationState persisted = new OAuthAuthorizationState(); persisted.create(userId, hash(state), PROVIDER, String.join(" ", SCOPES), expiresAt); states.save(persisted);
        String url = "https://accounts.google.com/o/oauth2/v2/auth?client_id=" + encode(properties.google().clientId()) + "&redirect_uri=" + encode(properties.google().redirectUri()) + "&response_type=code&access_type=offline&prompt=consent&include_granted_scopes=true&scope=" + encode(String.join(" ", SCOPES)) + "&state=" + encode(state);
        return new OAuthAuthorizationResponse(url, expiresAt);
    }
    @Transactional public void complete(String code, String state, String error) {
        if (error != null && !error.isBlank()) throw new IllegalArgumentException("Google authorization was not completed: " + error);
        if (code == null || state == null) throw new IllegalArgumentException("Google callback did not include an authorization code and state");
        OAuthAuthorizationState authorizationState = states.findByStateHash(hash(state)).orElseThrow(() -> new IllegalArgumentException("OAuth state is invalid or has expired"));
        if (authorizationState.getExpiresAt().isBefore(Instant.now())) { states.delete(authorizationState); throw new IllegalArgumentException("OAuth state has expired. Start the connection again."); }
        GoogleTokenResponse token = exchange(code); String encryptedAccess = cipher.encrypt(token.accessToken()); String encryptedRefresh = cipher.encrypt(token.refreshToken()); Instant expiry = token.expiresIn() == null ? null : Instant.now().plusSeconds(token.expiresIn());
        OAuthConnection connection = connections.findByUserIdAndProvider(authorizationState.getUserId(), PROVIDER).orElseGet(OAuthConnection::new);
        if (connection.getUserId() == null) connection.create(authorizationState.getUserId(), PROVIDER, encryptedAccess, encryptedRefresh, expiry, token.scope() == null ? authorizationState.getRequestedScopes() : token.scope()); else connection.update(encryptedAccess, encryptedRefresh, expiry, token.scope() == null ? authorizationState.getRequestedScopes() : token.scope());
        connections.save(connection); states.delete(authorizationState);
    }
    public boolean isConnected(UUID userId) { return connections.findByUserIdAndProvider(userId, PROVIDER).isPresent(); }
    @Transactional public String accessToken(UUID userId) {
        OAuthConnection connection = connections.findByUserIdAndProvider(userId, PROVIDER).orElseThrow(() -> new IllegalArgumentException("Connect Gmail before sending an application email."));
        if (connection.getExpiresAt() == null || connection.getExpiresAt().isAfter(Instant.now().plusSeconds(60))) return cipher.decrypt(connection.getEncryptedAccessToken());
        if (connection.getEncryptedRefreshToken() == null) throw new IllegalArgumentException("Reconnect Gmail to refresh its sending permission.");
        try {
            String form = "client_id=" + encode(properties.google().clientId()) + "&client_secret=" + encode(properties.google().clientSecret()) + "&refresh_token=" + encode(cipher.decrypt(connection.getEncryptedRefreshToken())) + "&grant_type=refresh_token";
            GoogleTokenResponse token = restClient.post().uri("https://oauth2.googleapis.com/token").contentType(MediaType.APPLICATION_FORM_URLENCODED).body(form).retrieve().body(GoogleTokenResponse.class);
            if (token == null || token.accessToken() == null) throw new IllegalArgumentException("Google could not refresh the Gmail connection.");
            connection.update(cipher.encrypt(token.accessToken()), token.refreshToken() == null ? null : cipher.encrypt(token.refreshToken()), token.expiresIn() == null ? null : Instant.now().plusSeconds(token.expiresIn()), token.scope() == null ? connection.getScopes() : token.scope()); connections.save(connection); return token.accessToken();
        } catch (RestClientException exception) { throw new IllegalArgumentException("Reconnect Gmail to refresh its sending permission."); }
    }
    private GoogleTokenResponse exchange(String code) { try { String form = "code=" + encode(code) + "&client_id=" + encode(properties.google().clientId()) + "&client_secret=" + encode(properties.google().clientSecret()) + "&redirect_uri=" + encode(properties.google().redirectUri()) + "&grant_type=authorization_code"; GoogleTokenResponse response = restClient.post().uri("https://oauth2.googleapis.com/token").contentType(MediaType.APPLICATION_FORM_URLENCODED).body(form).retrieve().body(GoogleTokenResponse.class); if (response == null || response.accessToken() == null) throw new IllegalArgumentException("Google did not return an access token"); return response; } catch (RestClientException e) { throw new IllegalArgumentException("Could not exchange the Google authorization code", e); } }
    private String randomState() { byte[] bytes = new byte[32]; random.nextBytes(bytes); return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes); }
    private String hash(String value) { try { byte[] hash = MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8)); return HexFormat.of().formatHex(hash); } catch (Exception e) { throw new IllegalStateException("Could not create OAuth state", e); } }
    private boolean nonBlank(String value) { return value != null && !value.isBlank(); }
    private String encode(String value) { return URLEncoder.encode(value, StandardCharsets.UTF_8); }
    private record GoogleTokenResponse(@com.fasterxml.jackson.annotation.JsonProperty("access_token") String accessToken, @com.fasterxml.jackson.annotation.JsonProperty("refresh_token") String refreshToken, @com.fasterxml.jackson.annotation.JsonProperty("expires_in") Long expiresIn, String scope) { }
}
