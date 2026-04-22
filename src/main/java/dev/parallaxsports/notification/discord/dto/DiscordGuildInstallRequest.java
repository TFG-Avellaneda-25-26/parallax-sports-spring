package dev.parallaxsports.notification.discord.dto;

import java.time.OffsetDateTime;

public record DiscordGuildInstallRequest(
    String ownerDiscordId,
    OffsetDateTime installedAt
) {}
