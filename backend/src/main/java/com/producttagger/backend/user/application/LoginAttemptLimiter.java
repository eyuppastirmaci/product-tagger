package com.producttagger.backend.user.application;

import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.Deque;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;

/**
 * In-memory brute-force brake: 5 failed attempts per key within 15 minutes.
 * Single-instance by design; swap the storage for Redis if the app ever scales
 * horizontally.
 */
@Component
public class LoginAttemptLimiter {

    private static final int MAX_ATTEMPTS = 5;
    private static final Duration WINDOW = Duration.ofMinutes(15);

    private final Map<String, Deque<Instant>> failures = new ConcurrentHashMap<>();

    public void check(String key) {
        Deque<Instant> attempts = failures.get(key);

        if (attempts == null) {
            return;
        }

        prune(attempts);

        if (attempts.size() >= MAX_ATTEMPTS) {
            throw new LoginRateLimitException();
        }
    }

    public void recordFailure(String key) {
        Deque<Instant> attempts = failures.computeIfAbsent(key, k -> new ConcurrentLinkedDeque<>());

        prune(attempts);
        attempts.addLast(Instant.now());
    }

    public void reset(String key) {
        failures.remove(key);
    }

    private void prune(Deque<Instant> attempts) {
        Instant cutoff = Instant.now().minus(WINDOW);

        while (!attempts.isEmpty() && attempts.peekFirst().isBefore(cutoff)) {
            attempts.pollFirst();
        }
    }
}
