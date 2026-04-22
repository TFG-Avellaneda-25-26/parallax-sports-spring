package dev.parallaxsports.notification.discord.dto;

import dev.parallaxsports.notification.discord.model.DiscordDeliveryMode;

public record DiscordDeliveryRequest(
    DiscordDeliveryMode mode,
    String guildId
) {}
