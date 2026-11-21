package com.applypilot.integration.service;

import com.applypilot.config.AppProperties;
import java.security.SecureRandom;
import java.util.Base64;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.*;

class OAuthTokenCipherTest {
    private OAuthTokenCipher cipher(byte[] key) { return new OAuthTokenCipher(new AppProperties(new AppProperties.Jwt("a-development-secret-at-least-thirty-two-bytes", 60), new AppProperties.Cors("http://localhost:3000"), new AppProperties.AiService("http://localhost:8000"), new AppProperties.Google("id", "secret", "http://localhost/callback"), new AppProperties.Frontend("http://localhost:3000"), new AppProperties.Security(Base64.getEncoder().encodeToString(key)), new AppProperties.WebSearch("", "", "gpt-5.4"), new AppProperties.Storage("storage/resumes"))); }
    @Test void encrypts_and_decrypts_a_token() { byte[] key = new byte[32]; new SecureRandom().nextBytes(key); OAuthTokenCipher cipher = cipher(key); String encrypted = cipher.encrypt("access-token-value"); assertThat(encrypted).doesNotContain("access-token-value"); assertThat(cipher.decrypt(encrypted)).isEqualTo("access-token-value"); }
    @Test void rejects_a_key_of_the_wrong_size() { assertThatThrownBy(() -> cipher(new byte[8]).encrypt("token")).isInstanceOf(IllegalArgumentException.class); }
}
