package dev.parallaxsports.notification.repository;

import dev.parallaxsports.notification.model.UserEventAlert;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserEventAlertRepository extends JpaRepository<UserEventAlert, Long>, UserEventAlertClaimRepository {

    List<UserEventAlert> findByStatusAndSendAtUtcLessThanEqualOrderBySendAtUtcAsc(
        String status,
        OffsetDateTime sendAtUtc,
        Pageable pageable
    );

    Optional<UserEventAlert> findByUser_IdAndEventIdAndChannelAndLeadTimeMinutes(
        Long userId,
        Long eventId,
        String channel,
        Integer leadTimeMinutes
    );

    List<UserEventAlert> findByStatusInAndSendAtUtcLessThanEqualOrderBySendAtUtcAsc(
        List<String> statuses,
        OffsetDateTime sendAtUtc,
        Pageable pageable
    );

    List<UserEventAlert> findByUser_IdOrderBySendAtUtcAsc(Long userId);
}
