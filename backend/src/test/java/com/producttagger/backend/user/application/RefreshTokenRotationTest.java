package com.producttagger.backend.user.application;

import com.producttagger.backend.IntegrationTest;
import com.producttagger.backend.user.domain.RefreshToken;
import com.producttagger.backend.user.domain.RefreshTokenRepository;
import com.producttagger.backend.user.domain.User;
import com.producttagger.backend.user.domain.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

class RefreshTokenRotationTest extends IntegrationTest {

    @Autowired
    private RefreshTokenService service;

    @Autowired
    private RefreshTokenRepository tokens;

    @Autowired
    private UserRepository users;

    /**
     * The single-use guarantee under concurrency: when two requests race with
     * the same token, exactly one wins the atomic claim and the loser triggers
     * reuse detection, which drops every session of the user.
     */
    @Test
    void concurrentRotationsYieldExactlyOneWinner() throws Exception {
        User user = newUser();
        String rawToken = service.issue(user);

        CyclicBarrier barrier = new CyclicBarrier(2);
        Callable<Object> attempt = () -> {
            barrier.await();

            try {
                return service.rotate(rawToken);
            } catch (InvalidRefreshTokenException e) {
                return e;
            }
        };

        ExecutorService pool = Executors.newFixedThreadPool(2);
        List<Future<Object>> results;
        try {
            results = pool.invokeAll(List.of(attempt, attempt));
        } finally {
            pool.shutdown();
        }

        long rotations = 0;
        long rejections = 0;
        for (Future<Object> result : results) {
            if (result.get() instanceof RefreshTokenService.Rotation) {
                rotations++;
            } else if (result.get() instanceof InvalidRefreshTokenException) {
                rejections++;
            }
        }

        assertThat(rotations).isEqualTo(1);
        assertThat(rejections).isEqualTo(1);

        // The losing request counts as reuse, so even the winner's fresh token
        // is revoked: a genuinely raced token is treated as leaked
        assertThat(activeTokensOf(user)).isZero();
    }

    @Test
    void reuseOfRotatedTokenDropsEverySession() {
        User user = newUser();
        String firstToken = service.issue(user);
        String otherDeviceToken = service.issue(user);

        String rotatedToken = service.rotate(firstToken).refreshToken();

        // Presenting the rotated-out token again is the leak signal
        assertThatExceptionOfType(InvalidRefreshTokenException.class)
                .isThrownBy(() -> service.rotate(firstToken));

        assertThat(activeTokensOf(user)).isZero();

        assertThatExceptionOfType(InvalidRefreshTokenException.class)
                .isThrownBy(() -> service.rotate(rotatedToken));
        assertThatExceptionOfType(InvalidRefreshTokenException.class)
                .isThrownBy(() -> service.rotate(otherDeviceToken));
    }

    @Test
    void expiredTokenCannotRotate() {
        User user = newUser();
        String rawToken = Base64.getUrlEncoder().withoutPadding().encodeToString(new byte[48]);

        tokens.save(RefreshToken.issue(user, sha256(rawToken), Instant.now().minusSeconds(60)));

        assertThatExceptionOfType(InvalidRefreshTokenException.class)
                .isThrownBy(() -> service.rotate(rawToken));

        assertThat(activeTokensOf(user)).isZero();
    }

    @Test
    void unknownTokenIsRejected() {
        assertThatExceptionOfType(InvalidRefreshTokenException.class)
                .isThrownBy(() -> service.rotate("never-issued"));
    }

    private User newUser() {
        return users.save(User.register("rotation-" + UUID.randomUUID() + "@test.local", "hash", "Rotation Tester"));
    }

    private long activeTokensOf(User user) {
        return tokens.findAll().stream()
                .filter(token -> token.getUser().getId().equals(user.getId()))
                .filter(token -> !token.isRevoked())
                .count();
    }

    private static String sha256(String rawToken) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");

            return HexFormat.of().formatHex(digest.digest(rawToken.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }
}
