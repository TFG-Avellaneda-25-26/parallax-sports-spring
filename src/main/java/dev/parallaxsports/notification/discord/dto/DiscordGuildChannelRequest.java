package dev.parallaxsports.notification.discord.dto;

public record DiscordGuildChannelRequest(
    String channelId,
    String sportKey,
    String setByDiscordUserId
) {}
