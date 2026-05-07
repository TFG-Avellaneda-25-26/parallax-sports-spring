package dev.parallaxsports.notification.discord.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "discord_guild_configs")
public class DiscordGuildConfig {

    @Id
    @Column(name = "guild_id", nullable = false)
    private String guildId;

    @Column(name = "default_channel_id")
    private String defaultChannelId;

    @Column(name = "installed_by_discord_user_id")
    private String installedByDiscordUserId;

    @Column(name = "installed_at", nullable = false)
    private OffsetDateTime installedAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    @PrePersist
    void onCreate() {
        OffsetDateTime now = OffsetDateTime.now();
        if (installedAt == null) {
            installedAt = now;
        }
        if (updatedAt == null) {
            updatedAt = now;
        }
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = OffsetDateTime.now();
    }
}
