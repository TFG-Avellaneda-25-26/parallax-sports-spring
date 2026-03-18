package dev.parallaxsports.formula1.repository;

import dev.parallaxsports.formula1.model.Competition;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CompetitionRepository extends JpaRepository<Competition, Long> {

    Optional<Competition> findBySportIdAndName(Long sportId, String name);
}
