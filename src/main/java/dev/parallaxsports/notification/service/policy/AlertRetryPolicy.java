package dev.parallaxsports.notification.service.policy;

import java.time.OffsetDateTime;
import org.springframework.stereotype.Component;

/**
 * Defines bounded retry backoff schedule for retryable alert failures.
 */
@Component
public class AlertRetryPolicy {

    private static final int[] SCHEDULE_MINUTES = {1, 3, 10, 30, 120, 480};

    /**
     * Computes next retry timestamp from attempt count and backoff schedule.
     *
     * @param now current timestamp
     * @param attempts 1-based attempt number (null treated as first attempt)
     * @return next retry timestamp
     */
    public OffsetDateTime computeNextRetryAt(OffsetDateTime now, Integer attempts) {
        int currentAttempts = attempts == null ? 1 : attempts;
        int index = Math.min(Math.max(currentAttempts - 1, 0), SCHEDULE_MINUTES.length - 1);
        return now.plusMinutes(SCHEDULE_MINUTES[index]);
    }
}