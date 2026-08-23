package com.producttagger.backend.shared.security;

import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;

class JwtServiceTest {

    private static final String SECRET = "test-only-secret-0123456789abcdef0123456789abcdef";

    private final JwtService service = new JwtService(properties(SECRET, Duration.ofMinutes(15)));

    @Test
    void issuedTokenRoundTrips() {
        AuthenticatedUser user = new AuthenticatedUser(42L, "user@test.local", "Test User");

        AuthenticatedUser parsed = service.parse(service.issue(user));

        assertThat(parsed).isEqualTo(user);
    }

    @Test
    void expiredTokenParsesToNull() {
        JwtService expiredIssuer = new JwtService(properties(SECRET, Duration.ofMinutes(-5)));

        String token = expiredIssuer.issue(new AuthenticatedUser(1L, "a@b.c", "A"));

        assertThat(service.parse(token)).isNull();
    }

    @Test
    void tamperedTokenParsesToNull() {
        String token = service.issue(new AuthenticatedUser(1L, "a@b.c", "A"));
        String tampered = token.substring(0, token.length() - 2) + "xx";

        assertThat(service.parse(tampered)).isNull();
        assertThat(service.parse("not-a-jwt")).isNull();
        assertThat(service.parse(null)).isNull();
    }

    @Test
    void tokenSignedWithAnotherSecretIsRejected() {
        JwtService other = new JwtService(
                properties("another-secret-0123456789abcdef0123456789abcdef", Duration.ofMinutes(15)));

        String token = other.issue(new AuthenticatedUser(1L, "a@b.c", "A"));

        assertThat(service.parse(token)).isNull();
    }

    @Test
    void propertiesRejectMissingOrWeakSecret() {
        assertThatIllegalStateException()
                .isThrownBy(() -> properties("", Duration.ofMinutes(15)))
                .withMessageContaining("JWT_SECRET");

        assertThatIllegalStateException()
                .isThrownBy(() -> properties("too-short", Duration.ofMinutes(15)))
                .withMessageContaining("32");
    }

    private static JwtProperties properties(String secret, Duration expiry) {
        return new JwtProperties(secret, expiry, Duration.ofDays(14), false);
    }
}
