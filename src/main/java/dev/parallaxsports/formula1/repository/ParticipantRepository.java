package dev.parallaxsports.formula1.repository;

import dev.parallaxsports.formula1.model.Participant;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ParticipantRepository extends JpaRepository<Participant, Long> {

    Optional<Participant> findBySportIdAndKindAndName(Long sportId, String kind, String name);
}
