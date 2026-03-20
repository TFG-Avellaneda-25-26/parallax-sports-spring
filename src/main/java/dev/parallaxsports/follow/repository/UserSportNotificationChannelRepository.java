package dev.parallaxsports.follow.repository;

import dev.parallaxsports.follow.model.UserSportNotificationChannel;
import dev.parallaxsports.follow.model.UserSportNotificationChannelId;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserSportNotificationChannelRepository
    extends JpaRepository<UserSportNotificationChannel, UserSportNotificationChannelId> {

    List<UserSportNotificationChannel> findByIdSportIdAndEnabledTrue(Long sportId);

    List<UserSportNotificationChannel> findByIdUserIdAndIdSportIdAndEnabledTrue(Long userId, Long sportId);
}
