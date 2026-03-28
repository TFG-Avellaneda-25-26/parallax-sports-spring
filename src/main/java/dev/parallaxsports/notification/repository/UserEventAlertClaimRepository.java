package dev.parallaxsports.notification.repository;

import java.time.OffsetDateTime;
import java.util.List;

/**
 * Contract for atomic claim-and-mark operations on due alerts.
 */
public interface UserEventAlertClaimRepository {

    /**
     * Claims due alerts for one channel using lock-safe semantics.
     *
     * @param channel delivery channel to claim (telegram/discord/email)
     * @param streamName target stream name recorded on claimed rows
     * @param now current time used as due boundary and queued/dispatched timestamp
     * @param batchSize maximum number of rows to claim
     * @return claimed alert ids
     */
    List<Long> claimDueAlertIdsForChannel(String channel, String streamName, OffsetDateTime now, int batchSize);
}
