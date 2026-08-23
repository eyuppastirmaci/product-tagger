package com.producttagger.backend.shared.security;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@ConfigurationProperties(prefix = "auth.jwt")
public record JwtProperties(String secret, Duration expiry, Duration refreshExpiry, boolean secureCookie) {

    // HS256 requires a key of at least 256 bits
    private static final int MIN_SECRET_CHARS = 32;

    public JwtProperties {
        if (secret == null || secret.isBlank()) {
            throw new IllegalStateException(
                    "JWT_SECRET must be set; generate one with: openssl rand -base64 48");
        }

        if (secret.length() < MIN_SECRET_CHARS) {
            throw new IllegalStateException(
                    "JWT_SECRET must be at least %d characters long".formatted(MIN_SECRET_CHARS));
        }
    }
}
