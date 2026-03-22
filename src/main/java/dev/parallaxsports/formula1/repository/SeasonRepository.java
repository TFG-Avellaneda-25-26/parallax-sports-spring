package dev.parallaxsports.formula1.repository;

import dev.parallaxsports.formula1.model.Season;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SeasonRepository extends JpaRepository<Season, Long> {

    Optional<Season> findByCompetitionIdAndName(Long competitionId, String name);
}
