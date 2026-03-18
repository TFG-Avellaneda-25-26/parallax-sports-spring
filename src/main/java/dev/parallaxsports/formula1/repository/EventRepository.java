package dev.parallaxsports.formula1.repository;

import dev.parallaxsports.formula1.model.Event;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EventRepository extends JpaRepository<Event, Long> {

    Optional<Event> findByExternalProviderAndExternalId(String externalProvider, String externalId);

    List<Event> findByExternalProviderAndExternalIdStartingWithAndSeasonIdOrderByStartTimeUtcAsc(
        String externalProvider,
        String externalIdPrefix,
        Long seasonId
    );
}
