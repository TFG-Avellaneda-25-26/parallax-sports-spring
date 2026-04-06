package dev.parallaxsports.auth.repository;

import dev.parallaxsports.auth.model.RefreshToken;
import java.time.OffsetDateTime;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, String> {

    @Modifying
    @Query("UPDATE RefreshToken r SET r.revokedAt = :now WHERE r.user.id = :userId AND r.revokedAt IS NULL")
    int revokeAllByUserId(@Param("userId") Long userId, @Param("now") OffsetDateTime now);

    @Modifying
    @Query("DELETE FROM RefreshToken r WHERE r.expiresAt < :expiryCutoff OR (r.revokedAt IS NOT NULL AND r.revokedAt < :revokedCutoff)")
    int deleteExpiredAndOldRevoked(
        @Param("expiryCutoff") OffsetDateTime expiryCutoff,
        @Param("revokedCutoff") OffsetDateTime revokedCutoff
    );
}
