package dev.parallaxsports.notification.repository;

import java.time.OffsetDateTime;
import java.util.List;

public interface UserEventAlertClaimRepository {

    List<Long> claimDueAlertIdsForChannel(String channel, String streamName, OffsetDateTime now, int batchSize);
}
