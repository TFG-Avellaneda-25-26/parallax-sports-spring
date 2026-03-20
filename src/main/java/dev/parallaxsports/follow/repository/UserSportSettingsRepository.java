package dev.parallaxsports.follow.repository;

import dev.parallaxsports.follow.model.UserSportSettings;
import dev.parallaxsports.follow.model.UserSportSettingsId;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserSportSettingsRepository extends JpaRepository<UserSportSettings, UserSportSettingsId> {

    Optional<UserSportSettings> findByIdUserIdAndIdSportId(Long userId, Long sportId);

    List<UserSportSettings> findByIdSportId(Long sportId);
}
