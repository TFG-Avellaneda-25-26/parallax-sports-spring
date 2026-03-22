package dev.parallaxsports.formula1.repository;

import dev.parallaxsports.formula1.model.Participant;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface ParticipantRepository extends JpaRepository<Participant, Long> {

    Optional<Participant> findBySportIdAndKindAndName(Long sportId, String kind, String name);

    @Query(value = """
        select *
        from participants
        where sport_id = :sportId
          and metadata ->> 'externalTeamId' = :externalTeamId
        limit 1
        """, nativeQuery = true)
    Optional<Participant> findBySportIdAndExternalTeamId(Long sportId, String externalTeamId);
}
