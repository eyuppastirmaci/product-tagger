package com.producttagger.backend.shared.security;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@ConfigurationProperties(prefix = "auth.jwt")
public record JwtProperties(String secret, Duration expiry, boolean secureCookie) {
}
