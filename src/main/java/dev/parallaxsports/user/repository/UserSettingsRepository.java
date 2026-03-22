package dev.parallaxsports.user.repository;

import dev.parallaxsports.user.model.UserSettings;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserSettingsRepository extends JpaRepository<UserSettings, Long> {
}
