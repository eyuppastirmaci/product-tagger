package com.producttagger.backend.shared.security;

import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

import java.time.Duration;

/**
 * The token travels in an httpOnly cookie (not an Authorization header) so
 * that img tags and EventSource, which cannot set headers, stay authenticated.
 */
@Component
public class AuthCookies {

    public static final String TOKEN_COOKIE = "pt-token";

    private final JwtProperties properties;

    AuthCookies(JwtProperties properties) {
        this.properties = properties;
    }

    public ResponseCookie session(String token) {
        return builder(token).maxAge(properties.expiry()).build();
    }

    public ResponseCookie expired() {
        return builder("").maxAge(Duration.ZERO).build();
    }

    private ResponseCookie.ResponseCookieBuilder builder(String value) {
        return ResponseCookie.from(TOKEN_COOKIE, value)
                .httpOnly(true)
                .secure(properties.secureCookie())
                .sameSite("Lax")
                .path("/");
    }
}
