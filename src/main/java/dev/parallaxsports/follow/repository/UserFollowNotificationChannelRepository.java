package dev.parallaxsports.follow.repository;

import dev.parallaxsports.follow.model.UserFollowNotificationChannel;
import dev.parallaxsports.follow.model.UserFollowNotificationChannelId;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserFollowNotificationChannelRepository
    extends JpaRepository<UserFollowNotificationChannel, UserFollowNotificationChannelId> {

    List<UserFollowNotificationChannel> findByIdFollowIdInAndEnabledTrue(List<Long> followIds);
}
