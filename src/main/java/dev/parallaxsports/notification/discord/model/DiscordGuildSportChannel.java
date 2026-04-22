package dev.parallaxsports.notification.discord.model;

import dev.parallaxsports.sport.model.Sport;
import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapsId;
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
@Table(name = "discord_guild_sport_channels")
public class DiscordGuildSportChannel {

    @EmbeddedId
    private DiscordGuildSportChannelId id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @MapsId("guildId")
    @JoinColumn(name = "guild_id", nullable = false)
    private DiscordGuildConfig guild;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @MapsId("sportId")
    @JoinColumn(name = "sport_id", nullable = false)
    private Sport sport;

    @Column(name = "channel_id", nullable = false)
    private String channelId;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    @PrePersist
    void onCreate() {
        OffsetDateTime now = OffsetDateTime.now();
        if (createdAt == null) {
            createdAt = now;
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
