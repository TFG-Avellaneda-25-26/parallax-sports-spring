package dev.parallaxsports.formula1.repository;

import dev.parallaxsports.formula1.model.EventEntry;
import dev.parallaxsports.formula1.model.EventEntryId;
import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface EventEntryRepository extends JpaRepository<EventEntry, EventEntryId> {

    boolean existsByIdEventIdAndIdParticipantId(Long eventId, Long participantId);

    @Query("SELECT ee FROM EventEntry ee JOIN FETCH ee.participant WHERE ee.id.eventId IN :eventIds")
    List<EventEntry> findWithParticipantByEventIdIn(@Param("eventIds") Collection<Long> eventIds);
}
