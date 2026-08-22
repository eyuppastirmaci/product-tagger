package com.producttagger.backend.user.application;

import com.producttagger.backend.user.domain.User;
import com.producttagger.backend.user.domain.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;

@Service
public class AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthService.class);

    private final UserRepository users;
    private final PasswordEncoder passwordEncoder;
    private final LoginAttemptLimiter attemptLimiter;

    public AuthService(UserRepository users, PasswordEncoder passwordEncoder, LoginAttemptLimiter attemptLimiter) {
        this.users = users;
        this.passwordEncoder = passwordEncoder;
        this.attemptLimiter = attemptLimiter;
    }

    @Transactional
    public User register(String name, String email, String rawPassword) {
        String normalizedEmail = normalize(email);

        if (users.existsByEmail(normalizedEmail)) {
            throw new EmailAlreadyUsedException();
        }

        User user = User.register(normalizedEmail, passwordEncoder.encode(rawPassword), name.trim());

        users.save(user);

        log.info("User {} registered", normalizedEmail);

        return user;
    }

    /**
     * The rate-limit key is email plus client IP, so one attacker cannot lock
     * a victim's account out and one IP cannot hammer many accounts.
     */
    public User authenticate(String email, String rawPassword, String clientIp) {
        String limiterKey = normalize(email) + "|" + clientIp;

        attemptLimiter.check(limiterKey);

        User user = users.findByEmail(normalize(email)).orElse(null);

        if (user == null || !passwordEncoder.matches(rawPassword, user.getPasswordHash())) {
            attemptLimiter.recordFailure(limiterKey);

            throw new InvalidCredentialsException();
        }

        attemptLimiter.reset(limiterKey);

        return user;
    }

    public User byId(Long id) {
        return users.findById(id).orElseThrow(InvalidCredentialsException::new);
    }

    private String normalize(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }
}
