package dev.parallaxsports.notification.repository;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.time.OffsetDateTime;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;

@Repository
@Slf4j
public class UserEventAlertClaimRepositoryImpl implements UserEventAlertClaimRepository {

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    @SuppressWarnings("unchecked")
    public List<Long> claimDueAlertIdsForChannel(String channel, String streamName, OffsetDateTime now, int batchSize) {
        String sql = """
            with due as (
                select id
                from user_event_alerts
                where channel = :channel
                  and status in ('scheduled', 'failed_retryable')
                  and coalesce(next_retry_at_utc, send_at_utc) <= :now
                  and (artifact_required = false or artifact_id is not null)
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
