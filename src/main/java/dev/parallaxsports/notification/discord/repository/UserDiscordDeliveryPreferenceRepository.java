package dev.parallaxsports.notification.discord.repository;

import dev.parallaxsports.notification.discord.model.UserDiscordDeliveryPreference;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserDiscordDeliveryPreferenceRepository
    extends JpaRepository<UserDiscordDeliveryPreference, Long> {
}
