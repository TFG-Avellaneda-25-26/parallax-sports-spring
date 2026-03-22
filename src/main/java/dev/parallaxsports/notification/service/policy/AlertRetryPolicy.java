package dev.parallaxsports.notification.service.policy;

import java.time.OffsetDateTime;
import java.util.concurrent.ThreadLocalRandom;
import org.springframework.stereotype.Component;

/**
 * Defines bounded retry backoff schedule with jitter for retryable alert failures.
 *
 * Jitter spreads retries across a +/-25% window around the base delay to prevent
 * thundering-herd spikes when many alerts fail simultaneously.
 */
@Component
public class AlertRetryPolicy {

    private static final int[] SCHEDULE_MINUTES = {1, 3, 10, 30, 120, 480};
    private static final double JITTER_FACTOR = 0.25;

    /**
     * Computes next retry timestamp from attempt count and backoff schedule with jitter.
     *
     * @param now current timestamp
     * @param attempts 1-based attempt number (null treated as first attempt)
     * @return next retry timestamp with jitter applied
     */
    public OffsetDateTime computeNextRetryAt(OffsetDateTime now, Integer attempts) {
        int currentAttempts = attempts == null ? 1 : attempts;
        int index = Math.min(Math.max(currentAttempts - 1, 0), SCHEDULE_MINUTES.length - 1);
        int baseMinutes = SCHEDULE_MINUTES[index];
        long jitteredSeconds = applyJitter(baseMinutes * 60L);
        return now.plusSeconds(jitteredSeconds);
    }

    private long applyJitter(long baseSeconds) {
        long jitterRange = (long) (baseSeconds * JITTER_FACTOR);
        if (jitterRange == 0) {
            return baseSeconds;
        }
        long offset = ThreadLocalRandom.current().nextLong(-jitterRange, jitterRange + 1);
        return Math.max(1, baseSeconds + offset);
    }
}
