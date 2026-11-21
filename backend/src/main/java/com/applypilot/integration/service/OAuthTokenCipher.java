package com.applypilot.integration.service;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;
import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.stereotype.Component;

/** Encrypts OAuth secrets before persistence; plaintext tokens never enter the database. */
@Component
public class OAuthTokenCipher {
    private static final SecureRandom RANDOM = new SecureRandom();
    private static final int NONCE_BYTES = 12;
    private final String encodedKey;
    public OAuthTokenCipher(com.applypilot.config.AppProperties properties) { encodedKey = properties.security().tokenEncryptionKey(); }
    public boolean isConfigured() { try { key(); return true; } catch (IllegalArgumentException ignored) { return false; } }
    public String encrypt(String plaintext) {
        if (plaintext == null || plaintext.isBlank()) return null;
        try { byte[] nonce = new byte[NONCE_BYTES]; RANDOM.nextBytes(nonce); Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding"); cipher.init(Cipher.ENCRYPT_MODE, key(), new GCMParameterSpec(128, nonce)); byte[] ciphertext = cipher.doFinal(plaintext.getBytes(StandardCharsets.UTF_8)); return Base64.getEncoder().encodeToString(ByteBuffer.allocate(nonce.length + ciphertext.length).put(nonce).put(ciphertext).array()); }
        catch (IllegalArgumentException e) { throw e; }
        catch (Exception e) { throw new IllegalStateException("Could not encrypt OAuth token", e); }
    }
    public String decrypt(String encrypted) {
        try { byte[] packed = Base64.getDecoder().decode(encrypted); if (packed.length <= NONCE_BYTES) throw new IllegalArgumentException("Stored OAuth token is invalid"); byte[] nonce = java.util.Arrays.copyOfRange(packed, 0, NONCE_BYTES); byte[] ciphertext = java.util.Arrays.copyOfRange(packed, NONCE_BYTES, packed.length); Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding"); cipher.init(Cipher.DECRYPT_MODE, key(), new GCMParameterSpec(128, nonce)); return new String(cipher.doFinal(ciphertext), StandardCharsets.UTF_8); }
        catch (IllegalArgumentException e) { throw e; }
        catch (Exception e) { throw new IllegalStateException("Could not decrypt OAuth token", e); }
    }
    private SecretKey key() { if (encodedKey == null || encodedKey.isBlank()) throw new IllegalArgumentException("OAuth is not configured: APP_TOKEN_ENCRYPTION_KEY must be a Base64-encoded 32-byte key"); byte[] bytes; try { bytes = Base64.getDecoder().decode(encodedKey); } catch (IllegalArgumentException e) { throw new IllegalArgumentException("APP_TOKEN_ENCRYPTION_KEY must be valid Base64", e); } if (bytes.length != 32) throw new IllegalArgumentException("APP_TOKEN_ENCRYPTION_KEY must decode to exactly 32 bytes"); return new SecretKeySpec(bytes, "AES"); }
}
