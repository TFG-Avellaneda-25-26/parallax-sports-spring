package dev.parallaxsports.notification.discord.service;

import dev.parallaxsports.notification.discord.model.DiscordDeliveryMode;
import dev.parallaxsports.notification.discord.model.DiscordGuildConfig;
import dev.parallaxsports.notification.discord.model.DiscordGuildSportChannel;
import dev.parallaxsports.notification.discord.model.UserDiscordDeliveryPreference;
import dev.parallaxsports.notification.discord.model.UserDiscordSportDeliveryOverride;
import dev.parallaxsports.notification.discord.repository.DiscordGuildConfigRepository;
import dev.parallaxsports.notification.discord.repository.DiscordGuildSportChannelRepository;
import dev.parallaxsports.notification.discord.repository.UserDiscordDeliveryPreferenceRepository;
import dev.parallaxsports.notification.discord.repository.UserDiscordSportDeliveryOverrideRepository;
import dev.parallaxsports.user.model.UserIdentity;
import dev.parallaxsports.user.repository.UserIdentityRepository;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Resolves the destination for a discord alert given (userId, sportId).
 *
 * Resolution order:
 * 1. {@link UserDiscordSportDeliveryOverride} for (userId, sportId).
 * 2. {@link UserDiscordDeliveryPreference} for userId.
 * 3. No preference rows -> fall back to {@code GUILD_CHANNEL} against the only installed guild.
 *
 * For {@code GUILD_CHANNEL} mode the channel is resolved as:
 * a) {@link DiscordGuildSportChannel} for (guildId, sportId) — per-sport override.
 * b) {@link DiscordGuildConfig#getDefaultChannelId()} — guild default.
 *
 * Any step that fails produces an unroutable result with reason {@code discord_unroutable}.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class DiscordRoutingResolver {

    public static final String REASON_UNROUTABLE = "discord_unroutable";
    private static final String DISCORD_PROVIDER = "discord";

    private final UserDiscordSportDeliveryOverrideRepository overrideRepository;
    private final UserDiscordDeliveryPreferenceRepository preferenceRepository;
    private final DiscordGuildSportChannelRepository guildSportChannelRepository;
    private final DiscordGuildConfigRepository guildConfigRepository;
    private final UserIdentityRepository userIdentityRepository;

    public DiscordRouting resolve(Long userId, Long sportId) {
        String discordUserId = userIdentityRepository
            .findByUser_IdAndProvider(userId, DISCORD_PROVIDER)
            .map(UserIdentity::getProviderSubject)
            .orElse(null);
        if (discordUserId == null) {
            return DiscordRouting.unroutable(REASON_UNROUTABLE);
        }

        Optional<UserDiscordSportDeliveryOverride> override =
            overrideRepository.findByIdUserIdAndIdSportId(userId, sportId);
        if (override.isPresent()) {
            return resolveForPreference(override.get().getMode(), override.get().getGuildId(), sportId, discordUserId);
        }

        Optional<UserDiscordDeliveryPreference> preference = preferenceRepository.findById(userId);
        if (preference.isPresent()) {
            return resolveForPreference(preference.get().getMode(), preference.get().getGuildId(), sportId, discordUserId);
        }

        return resolveForGuildChannel(null, sportId, discordUserId);
    }

    private DiscordRouting resolveForPreference(
        DiscordDeliveryMode mode,
        String explicitGuildId,
        Long sportId,
        String discordUserId
    ) {
        if (mode == DiscordDeliveryMode.DM) {
            return DiscordRouting.routable(DiscordDeliveryMode.DM, discordUserId, null, explicitGuildId);
        }
        return resolveForGuildChannel(explicitGuildId, sportId, discordUserId);
    }

    private DiscordRouting resolveForGuildChannel(String explicitGuildId, Long sportId, String discordUserId) {
        DiscordGuildConfig guild = selectGuild(explicitGuildId);
        if (guild == null) {
            return DiscordRouting.unroutable(REASON_UNROUTABLE);
        }

        Optional<DiscordGuildSportChannel> perSport =
            guildSportChannelRepository.findByIdGuildIdAndIdSportId(guild.getGuildId(), sportId);
        if (perSport.isPresent()) {
            return DiscordRouting.routable(
                DiscordDeliveryMode.GUILD_CHANNEL,
                discordUserId,
                perSport.get().getChannelId(),
                guild.getGuildId()
            );
        }

        if (guild.getDefaultChannelId() != null && !guild.getDefaultChannelId().isBlank()) {
            return DiscordRouting.routable(
                DiscordDeliveryMode.GUILD_CHANNEL,
                discordUserId,
                guild.getDefaultChannelId(),
                guild.getGuildId()
            );
        }

        return DiscordRouting.unroutable(REASON_UNROUTABLE);
    }

    private DiscordGuildConfig selectGuild(String explicitGuildId) {
        if (explicitGuildId != null && !explicitGuildId.isBlank()) {
            return guildConfigRepository.findById(explicitGuildId).orElse(null);
        }
        List<DiscordGuildConfig> guilds = guildConfigRepository.findAll();
        if (guilds.size() == 1) {
            return guilds.getFirst();
        }
        return null;
    }

}
