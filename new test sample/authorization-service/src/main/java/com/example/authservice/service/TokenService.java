package com.example.authservice.service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.OffsetDateTime;
import java.util.Locale;
import java.util.HexFormat;
import org.springframework.stereotype.Service;

@Service
public class TokenService {

    private static final SecureRandom RANDOM = new SecureRandom();
    private static final int TOKEN_BYTES = 32;

    public String generateToken() {
        byte[] bytes = new byte[TOKEN_BYTES];
        RANDOM.nextBytes(bytes);
        return HexFormat.of().formatHex(bytes);
    }

    public String generateOtpCode() {
        int value = RANDOM.nextInt(1_000_000);
        return String.format(Locale.ROOT, "%06d", value);
    }

    public String hashToken(String rawToken) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(rawToken.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 algorithm is not available.", exception);
        }
    }

    public String hashOtpCode(String otpCode) {
        return hashToken(otpCode);
    }

    public OffsetDateTime accessExpiry() {
        return OffsetDateTime.now().plusMinutes(15);
    }

    public OffsetDateTime refreshExpiry() {
        return OffsetDateTime.now().plusDays(7);
    }
}
