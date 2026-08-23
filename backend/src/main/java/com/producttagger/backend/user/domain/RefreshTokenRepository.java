package com.producttagger.backend.user.domain;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.time.Instant;
import java.util.Optional;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {

    // The user is read outside the rotation transaction, so fetch it eagerly
    @EntityGraph(attributePaths = "user")
    Optional<RefreshToken> findByTokenHash(String tokenHash);

    /**
     * Atomic single-use claim: only one of any concurrent rotations can flip
     * {@code revoked_at} from null, so the returned count decides the winner.
     */
    @Modifying
    @Query("update RefreshToken t set t.revokedAt = :now where t.tokenHash = :tokenHash and t.revokedAt is null")
    int revokeIfActive(String tokenHash, Instant now);

    // Reuse detection response: drop every active session of the user
    @Modifying
    @Query("update RefreshToken t set t.revokedAt = :now where t.user.id = :userId and t.revokedAt is null")
    int revokeAllForUser(Long userId, Instant now);

    @Modifying
    @Query("delete from RefreshToken t where t.expiresAt < :cutoff")
    int deleteExpiredBefore(Instant cutoff);
}
