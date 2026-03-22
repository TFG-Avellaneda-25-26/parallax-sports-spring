package dev.parallaxsports.sport.repository;

import dev.parallaxsports.sport.model.Participant;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ParticipantRepository extends JpaRepository<Participant, Long> {

    Optional<Participant> findBySportIdAndKindAndName(Long sportId, String kind, String name);
}
