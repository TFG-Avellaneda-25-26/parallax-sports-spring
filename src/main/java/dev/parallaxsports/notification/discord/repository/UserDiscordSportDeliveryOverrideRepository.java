package dev.parallaxsports.notification.discord.repository;

import dev.parallaxsports.notification.discord.model.UserDiscordSportDeliveryOverride;
import dev.parallaxsports.notification.discord.model.UserDiscordSportDeliveryOverrideId;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserDiscordSportDeliveryOverrideRepository
    extends JpaRepository<UserDiscordSportDeliveryOverride, UserDiscordSportDeliveryOverrideId> {

    Optional<UserDiscordSportDeliveryOverride> findByIdUserIdAndIdSportId(Long userId, Long sportId);
}
