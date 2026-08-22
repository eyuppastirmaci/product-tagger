package com.producttagger.backend.shared.security;

import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;

@Component
public class JwtService {

    private final SecretKey key;
    private final JwtProperties properties;

    JwtService(JwtProperties properties) {
        this.key = Keys.hmacShaKeyFor(properties.secret().getBytes(StandardCharsets.UTF_8));
        this.properties = properties;
    }

    public String issue(AuthenticatedUser user) {
        Instant now = Instant.now();

        return Jwts.builder()
                .subject(String.valueOf(user.id()))
                .claim("email", user.email())
                .claim("name", user.name())
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plus(properties.expiry())))
                .signWith(key)
                .compact();
    }

    /**
     * Returns the authenticated user, or null for any invalid/expired token;
     * the caller treats null as an anonymous request.
     */
    public AuthenticatedUser parse(String token) {
        try {
            var claims = Jwts.parser()
                    .verifyWith(key)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();

            return new AuthenticatedUser(
                    Long.valueOf(claims.getSubject()),
                    claims.get("email", String.class),
                    claims.get("name", String.class));
        } catch (JwtException | IllegalArgumentException e) {
            return null;
        }
    }
}
