package dev.parallaxsports.notification.repository;

import dev.parallaxsports.notification.model.UserEventAlert;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Repository for alert lifecycle rows used by generation, dispatch, and callbacks.
 */
public interface UserEventAlertRepository extends JpaRepository<UserEventAlert, Long>, UserEventAlertClaimRepository {

    /**
     * Returns due alerts with one status ordered by send time.
     *
     * @param status target status
     * @param sendAtUtc due boundary
     * @param pageable page/limit configuration
     * @return ordered alerts matching status and due time
     */
    List<UserEventAlert> findByStatusAndSendAtUtcLessThanEqualOrderBySendAtUtcAsc(
        String status,
        OffsetDateTime sendAtUtc,
        Pageable pageable
    );

    /**
     * Finds the logical alert identity used for idempotent upsert.
     *
     * @param userId user id
     * @param eventId event id
     * @param channel delivery channel
     * @param leadTimeMinutes lead-time bucket
     * @return existing alert when present
     */
    Optional<UserEventAlert> findByUser_IdAndEventIdAndChannelAndLeadTimeMinutes(
        Long userId,
        Long eventId,
        String channel,
        Integer leadTimeMinutes
    );

    /**
     * Returns due alerts across multiple statuses ordered by send time.
     *
     * @param statuses statuses to include
     * @param sendAtUtc due boundary
     * @param pageable page/limit configuration
     * @return ordered due alerts
     */
    List<UserEventAlert> findByStatusInAndSendAtUtcLessThanEqualOrderBySendAtUtcAsc(
        List<String> statuses,
        OffsetDateTime sendAtUtc,
        Pageable pageable
    );

    /**
     * Returns user alerts sorted chronologically by intended send time.
     *
     * @param userId user id
     * @return alerts for the user ordered by send time
     */
    List<UserEventAlert> findByUser_IdOrderBySendAtUtcAsc(Long userId);
}
