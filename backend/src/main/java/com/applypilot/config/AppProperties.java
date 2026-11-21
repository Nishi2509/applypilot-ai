package com.applypilot.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app")
public record AppProperties(Jwt jwt, Cors cors, AiService aiService, Google google, Frontend frontend, Security security, WebSearch webSearch, Storage storage) {
    public record Jwt(String secret, long expirationMinutes) { }
    public record Cors(String allowedOrigins) { }
    public record AiService(String baseUrl) { }
    public record Google(String clientId, String clientSecret, String redirectUri) { }
    public record Frontend(String baseUrl) { }
    public record Security(String tokenEncryptionKey) { }
    public record WebSearch(String tavilyApiKey, String openaiApiKey, String openaiModel) { }
    public record Storage(String resumesDir) { }
}
