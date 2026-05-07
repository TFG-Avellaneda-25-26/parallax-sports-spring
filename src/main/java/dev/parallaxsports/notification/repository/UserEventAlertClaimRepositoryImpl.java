package dev.parallaxsports.notification.repository;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.time.OffsetDateTime;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;

/**
 * Native SQL implementation of lock-safe due-alert claiming.
 *
 * Uses FOR UPDATE SKIP LOCKED to support concurrent notification microservice
 * instances without
 * double-claiming the same alert rows.
 */
@Repository
@Slf4j
public class UserEventAlertClaimRepositoryImpl implements UserEventAlertClaimRepository {

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    @SuppressWarnings("unchecked")
    /**
     * Claims due alerts for a channel and marks them queued in one SQL statement.
     *
     * @param channel delivery channel to claim
     * @param streamName stream name recorded for downstream correlation
     * @param now due boundary and queue timestamp
     * @param batchSize maximum rows claimed in this call
     * @return claimed alert ids
     */
    public List<Long> claimDueAlertIdsForChannel(String channel, String streamName, OffsetDateTime now, int batchSize) {
        String sql = """
            with due as (
                select id
                from user_event_alerts
                where channel = :channel
                  and status in ('scheduled', 'failed_retryable')
                  and coalesce(next_retry_at_utc, send_at_utc) <= :now
                order by coalesce(next_retry_at_utc, send_at_utc), id
                for update skip locked
                limit :batchSize
            )
            update user_event_alerts a
               set status = 'queued',
                   queued_at_utc = :now,
                   dispatched_at_utc = :now,
                   stream_name = :streamName,
                   last_error = null,
                   last_error_code = null
              from due
             where a.id = due.id
            returning a.id
            """;

        return entityManager.createNativeQuery(sql)
            .setParameter("channel", channel)
            .setParameter("streamName", streamName)
            .setParameter("now", now)
            .setParameter("batchSize", batchSize)
            .getResultList();
    }
}
