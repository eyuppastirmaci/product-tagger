package com.producttagger.backend.shared.security;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

import java.time.Duration;

/**
 * The token travels in an httpOnly cookie (not an Authorization header) so
 * that img tags and EventSource, which cannot set headers, stay authenticated.
 */
@Component
public class AuthCookies {

    private static final Logger log = LoggerFactory.getLogger(AuthCookies.class);

    public static final String TOKEN_COOKIE = "pt-token";
    public static final String REFRESH_COOKIE = "pt-refresh";

    // The refresh cookie only ever travels to the auth endpoints
    private static final String REFRESH_PATH = "/api/auth";

    private final JwtProperties properties;

    AuthCookies(JwtProperties properties) {
        this.properties = properties;

        if (!properties.secureCookie()) {
            log.warn("Auth cookies are issued without the Secure flag; set SECURE_COOKIE=true behind HTTPS");
        }
    }

    public ResponseCookie session(String token) {
        return builder(TOKEN_COOKIE, token, "/").maxAge(properties.expiry()).build();
    }

    public ResponseCookie refresh(String token) {
        return builder(REFRESH_COOKIE, token, REFRESH_PATH).maxAge(properties.refreshExpiry()).build();
    }

    public ResponseCookie expired() {
        return builder(TOKEN_COOKIE, "", "/").maxAge(Duration.ZERO).build();
    }

    public ResponseCookie expiredRefresh() {
        return builder(REFRESH_COOKIE, "", REFRESH_PATH).maxAge(Duration.ZERO).build();
    }

    private ResponseCookie.ResponseCookieBuilder builder(String name, String value, String path) {
        return ResponseCookie.from(name, value)
                .httpOnly(true)
                .secure(properties.secureCookie())
                .sameSite("Lax")
                .path(path);
    }
}
