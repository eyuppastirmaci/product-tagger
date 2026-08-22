package com.producttagger.backend.user.application;

import com.producttagger.backend.shared.security.JwtProperties;
import com.producttagger.backend.user.domain.RefreshToken;
import com.producttagger.backend.user.domain.RefreshTokenRepository;
import com.producttagger.backend.user.domain.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;

@Service
public class RefreshTokenService {

    private static final Logger log = LoggerFactory.getLogger(RefreshTokenService.class);
    private static final SecureRandom RANDOM = new SecureRandom();
    private static final int TOKEN_BYTES = 48;

    private final RefreshTokenRepository tokens;
    private final JwtProperties properties;

    public RefreshTokenService(RefreshTokenRepository tokens, JwtProperties properties) {
        this.tokens = tokens;
        this.properties = properties;
    }

    @Transactional
    public String issue(User user) {
        String rawToken = randomToken();

        tokens.save(RefreshToken.issue(user, hash(rawToken), Instant.now().plus(properties.refreshExpiry())));

        return rawToken;
    }

    /**
     * Single-use rotation: the presented token is revoked and a fresh one is
     * issued. A token that was already revoked means it leaked and is being
     * reused, so every session of that user is dropped.
     */
    // noRollbackFor keeps the revoke-all update when the reuse branch throws
    @Transactional(noRollbackFor = InvalidRefreshTokenException.class)
    public Rotation rotate(String rawToken) {
        RefreshToken token = tokens.findByTokenHash(hash(rawToken))
                .orElseThrow(InvalidRefreshTokenException::new);

        if (token.isRevoked()) {
            log.warn("Refresh token reuse detected for user {}; revoking all sessions", token.getUser().getId());

            tokens.revokeAllForUser(token.getUser().getId(), Instant.now());

            throw new InvalidRefreshTokenException();
        }

        if (token.isExpired()) {
            throw new InvalidRefreshTokenException();
        }

        token.revoke();

        User user = token.getUser();
        String newRawToken = randomToken();

        tokens.save(RefreshToken.issue(user, hash(newRawToken), Instant.now().plus(properties.refreshExpiry())));

        return new Rotation(user, newRawToken);
    }

    @Transactional
    public void revoke(String rawToken) {
        tokens.findByTokenHash(hash(rawToken)).ifPresent(RefreshToken::revoke);
    }

    // Nightly cleanup; revoked rows stay until they expire (needed for reuse detection)
    @Scheduled(cron = "0 0 4 * * *")
    @Transactional
    public void purgeExpired() {
        int deleted = tokens.deleteExpiredBefore(Instant.now());

        if (deleted > 0) {
            log.info("Purged {} expired refresh tokens", deleted);
        }
    }

    private static String randomToken() {
        byte[] bytes = new byte[TOKEN_BYTES];

        RANDOM.nextBytes(bytes);

        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    // Plain SHA-256 is enough: the token itself is 384 bits of entropy
    private static String hash(String rawToken) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");

            return HexFormat.of().formatHex(digest.digest(rawToken.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 unavailable", e);
        }
    }

    public record Rotation(User user, String refreshToken) {
    }
}
