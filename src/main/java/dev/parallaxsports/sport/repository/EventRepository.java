package dev.parallaxsports.sport.repository;

import dev.parallaxsports.sport.model.Event;
import java.time.OffsetDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface EventRepository extends JpaRepository<Event, Long> {

    Optional<Event> findByExternalProviderAndExternalId(String externalProvider, String externalId);

    List<Event> findByExternalProviderAndExternalIdStartingWithAndSeasonIdOrderByStartTimeUtcAsc(
        String externalProvider,
        String externalIdPrefix,
        Long seasonId
    );

    Optional<Event> findFirstByExternalProviderAndEventTypeOrderByStartTimeUtcDesc(String externalProvider, String eventType);

    Optional<Event> findFirstByExternalProviderOrderByStartTimeUtcDesc(String externalProvider);

    List<Event> findByExternalProviderAndEventTypeAndStartTimeUtcBetweenOrderByStartTimeUtcAsc(
        String externalProvider,
        String eventType,
        OffsetDateTime from,
        OffsetDateTime to
    );

    List<Event> findByExternalProviderAndStartTimeUtcBetweenOrderByStartTimeUtcAsc(
        String externalProvider,
        OffsetDateTime from,
        OffsetDateTime to
    );

    @Query("""
        SELECT e FROM Event e LEFT JOIN FETCH e.competition
        WHERE e.externalProvider = :provider
          AND e.startTimeUtc BETWEEN :from AND :to
        ORDER BY e.startTimeUtc ASC
        """)
    List<Event> findWithCompetitionByProviderAndTimeBetween(
        @Param("provider") String provider,
        @Param("from") OffsetDateTime from,
        @Param("to") OffsetDateTime to
    );

    @Query("""
        SELECT e FROM Event e LEFT JOIN FETCH e.competition
        WHERE e.externalProvider IN :providers
          AND e.startTimeUtc BETWEEN :from AND :to
        ORDER BY e.startTimeUtc ASC
        """)
    List<Event> findWithCompetitionByProvidersAndTimeBetween(
        @Param("providers") Collection<String> providers,
        @Param("from") OffsetDateTime from,
        @Param("to") OffsetDateTime to
    );

    @Query("""
        SELECT e FROM Event e
        JOIN FETCH e.sport
        LEFT JOIN FETCH e.competition
        LEFT JOIN FETCH e.venue
        WHERE e.startTimeUtc BETWEEN :from AND :to
        ORDER BY e.startTimeUtc ASC, e.id ASC
        """)
    List<Event> findAllByTimeBetween(
        @Param("from") OffsetDateTime from,
        @Param("to") OffsetDateTime to,
        Pageable pageable
    );

    @Query("""
        SELECT e FROM Event e
        JOIN FETCH e.sport
        LEFT JOIN FETCH e.competition
        LEFT JOIN FETCH e.venue
        WHERE e.startTimeUtc BETWEEN :from AND :to
          AND (e.startTimeUtc > (SELECT ce.startTimeUtc FROM Event ce WHERE ce.id = :afterId)
               OR (e.startTimeUtc = (SELECT ce.startTimeUtc FROM Event ce WHERE ce.id = :afterId)
                   AND e.id > :afterId))
        ORDER BY e.startTimeUtc ASC, e.id ASC
        """)
    List<Event> findAllByTimeBetweenAfterCursor(
        @Param("from") OffsetDateTime from,
        @Param("to") OffsetDateTime to,
        @Param("afterId") Long afterId,
        Pageable pageable
    );
}
