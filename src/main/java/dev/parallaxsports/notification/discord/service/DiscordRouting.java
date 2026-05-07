package dev.parallaxsports.notification.discord.service;

import dev.parallaxsports.notification.discord.model.DiscordDeliveryMode;

/**
 * Result of resolving where a discord alert should be delivered.
 *
 * Either the alert is routable ({@link #routable(DiscordDeliveryMode, String, String, String)})
 * or unroutable with a stable reason code consumed by the dispatcher and log.
 */
public record DiscordRouting(
    DiscordDeliveryMode mode,
    String discordUserId,
    String discordChannelId,
    String discordGuildId,
    String unroutableReason
) {

    public boolean isRoutable() {
        return unroutableReason == null;
    }

    public static DiscordRouting routable(
        DiscordDeliveryMode mode,
        String discordUserId,
        String discordChannelId,
        String discordGuildId
    ) {
        return new DiscordRouting(mode, discordUserId, discordChannelId, discordGuildId, null);
    }

    public static DiscordRouting unroutable(String reason) {
        return new DiscordRouting(null, null, null, null, reason);
    }
}
