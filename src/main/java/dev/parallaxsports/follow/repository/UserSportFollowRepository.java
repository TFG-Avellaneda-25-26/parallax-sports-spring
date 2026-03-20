package dev.parallaxsports.follow.repository;

import dev.parallaxsports.follow.model.UserSportFollow;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserSportFollowRepository extends JpaRepository<UserSportFollow, Long> {

    List<UserSportFollow> findBySportIdAndNotifyTrue(Long sportId);

    List<UserSportFollow> findByUser_IdAndSportId(Long userId, Long sportId);

    Optional<UserSportFollow> findByUser_IdAndSportIdAndCompetitionId(Long userId, Long sportId, Long competitionId);

    Optional<UserSportFollow> findByUser_IdAndSportIdAndParticipantId(Long userId, Long sportId, Long participantId);
}
