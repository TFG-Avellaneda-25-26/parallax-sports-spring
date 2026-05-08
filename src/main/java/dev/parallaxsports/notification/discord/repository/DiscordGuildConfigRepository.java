package dev.parallaxsports.notification.discord.repository;

import dev.parallaxsports.notification.discord.model.DiscordGuildConfig;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DiscordGuildConfigRepository extends JpaRepository<DiscordGuildConfig, String> {
}
