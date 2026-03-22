package dev.parallaxsports.formula1.repository;

import dev.parallaxsports.formula1.model.EventEntry;
import dev.parallaxsports.formula1.model.EventEntryId;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EventEntryRepository extends JpaRepository<EventEntry, EventEntryId> {

    boolean existsByIdEventIdAndIdParticipantId(Long eventId, Long participantId);
}
