package dev.parallaxsports.auth.service;

import dev.parallaxsports.auth.repository.RefreshTokenRepository;
import java.time.OffsetDateTime;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
@Slf4j
public class RefreshTokenCleanupScheduler {

    private final RefreshTokenRepository refreshTokenRepository;

    /**
     * Runs daily at 03:00 UTC.
     * Deletes rows that are either expired or were revoked more than 30 days ago.
     */
    @Scheduled(cron = "0 0 3 * * *")
    @Transactional
    public void cleanupExpiredAndOldRevoked() {
        OffsetDateTime now = OffsetDateTime.now();
        OffsetDateTime revokedCutoff = now.minusDays(30);

        int deleted = refreshTokenRepository.deleteExpiredAndOldRevoked(now, revokedCutoff);
        if (deleted > 0) {
            log.info("Refresh token cleanup: deleted {} expired/old-revoked rows", deleted);
        }
    }
}
