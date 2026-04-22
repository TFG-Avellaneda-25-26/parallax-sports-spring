package dev.parallaxsports.notification.discord.repository;

import dev.parallaxsports.notification.discord.model.DiscordGuildSportChannel;
import dev.parallaxsports.notification.discord.model.DiscordGuildSportChannelId;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DiscordGuildSportChannelRepository
    extends JpaRepository<DiscordGuildSportChannel, DiscordGuildSportChannelId> {

    Optional<DiscordGuildSportChannel> findByIdGuildIdAndIdSportId(String guildId, Long sportId);
}
