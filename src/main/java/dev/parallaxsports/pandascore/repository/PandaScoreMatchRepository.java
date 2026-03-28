package dev.parallaxsports.pandascore.repository;

import dev.parallaxsports.pandascore.model.PandaScoreMatch;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PandaScoreMatchRepository extends JpaRepository<PandaScoreMatch, Long> {

    Optional<PandaScoreMatch> findByPandascoreId(Long pandascoreId);

    List<PandaScoreMatch> findByLeagueNameOrderByBeginAtDesc(String leagueName);

    List<PandaScoreMatch> findByVideogameIgnoreCaseOrderByBeginAtDesc(String videogame);

    List<PandaScoreMatch> findByBeginAtBetweenOrderByBeginAtDesc(OffsetDateTime beginAtStart, OffsetDateTime beginAtEnd);

    List<PandaScoreMatch> findAllByOrderByBeginAtDesc();
}


