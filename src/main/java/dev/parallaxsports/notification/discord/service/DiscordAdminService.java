package dev.parallaxsports.notification.discord.service;

import dev.parallaxsports.core.exception.BadRequestException;
import dev.parallaxsports.core.exception.ResourceNotFoundException;
import dev.parallaxsports.notification.discord.dto.DiscordDeliveryRequest;
import dev.parallaxsports.notification.discord.dto.DiscordGuildChannelRequest;
import dev.parallaxsports.notification.discord.dto.DiscordGuildInstallRequest;
import dev.parallaxsports.notification.discord.model.DiscordDeliveryMode;
import dev.parallaxsports.notification.discord.model.DiscordGuildConfig;
import dev.parallaxsports.notification.discord.model.DiscordGuildSportChannel;
import dev.parallaxsports.notification.discord.model.DiscordGuildSportChannelId;
import dev.parallaxsports.notification.discord.model.UserDiscordDeliveryPreference;
import dev.parallaxsports.notification.discord.model.UserDiscordSportDeliveryOverride;
import dev.parallaxsports.notification.discord.model.UserDiscordSportDeliveryOverrideId;
import dev.parallaxsports.notification.discord.repository.DiscordGuildConfigRepository;
import dev.parallaxsports.notification.discord.repository.DiscordGuildSportChannelRepository;
import dev.parallaxsports.notification.discord.repository.UserDiscordDeliveryPreferenceRepository;
import dev.parallaxsports.notification.discord.repository.UserDiscordSportDeliveryOverrideRepository;
import dev.parallaxsports.sport.model.Sport;
import dev.parallaxsports.sport.repository.SportRepository;
import dev.parallaxsports.user.model.User;
import dev.parallaxsports.user.repository.UserIdentityRepository;
import dev.parallaxsports.user.repository.UserRepository;
import java.time.OffsetDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class DiscordAdminService {

    private static final String DISCORD_PROVIDER = "discord";

    private final DiscordGuildConfigRepository guildConfigRepository;
    private final DiscordGuildSportChannelRepository guildSportChannelRepository;
    private final UserDiscordDeliveryPreferenceRepository preferenceRepository;
    private final UserDiscordSportDeliveryOverrideRepository overrideRepository;
    private final SportRepository sportRepository;
    private final UserRepository userRepository;
    private final UserIdentityRepository userIdentityRepository;

    public Long resolveUserIdByDiscordSnowflake(String discordUserId) {
        return userIdentityRepository
            .findByProviderAndProviderSubject(DISCORD_PROVIDER, discordUserId)
            .map(identity -> identity.getUser().getId())
            .orElseThrow(() -> new ResourceNotFoundException(
                "No Parallax account linked to discord user " + discordUserId
            ));
    }

    @Transactional
    public void installGuild(String guildId, DiscordGuildInstallRequest request) {
        DiscordGuildConfig guild = guildConfigRepository.findById(guildId)
            .orElseGet(() -> DiscordGuildConfig.builder().guildId(guildId).build());
        guild.setInstalledByDiscordUserId(request.ownerDiscordId());
        if (guild.getInstalledAt() == null) {
            guild.setInstalledAt(request.installedAt() != null ? request.installedAt() : OffsetDateTime.now());
        }
        guildConfigRepository.save(guild);
    }

    @Transactional
    public void uninstallGuild(String guildId) {
        guildConfigRepository.deleteById(guildId);
    }

    @Transactional
    public void upsertGuildChannel(String guildId, DiscordGuildChannelRequest request) {
        if (request.channelId() == null || request.channelId().isBlank()) {
            throw new BadRequestException("channelId is required");
        }

        DiscordGuildConfig guild = guildConfigRepository.findById(guildId)
            .orElseThrow(() -> new ResourceNotFoundException("Discord guild not installed: " + guildId));

        if (request.sportKey() == null || request.sportKey().isBlank()) {
            guild.setDefaultChannelId(request.channelId());
            guildConfigRepository.save(guild);
            return;
        }

        Sport sport = sportRepository.findByKey(request.sportKey())
            .orElseThrow(() -> new ResourceNotFoundException("Sport not found: " + request.sportKey()));

        DiscordGuildSportChannelId id = new DiscordGuildSportChannelId(guildId, sport.getId());
        DiscordGuildSportChannel row = guildSportChannelRepository.findById(id)
            .orElseGet(() -> DiscordGuildSportChannel.builder()
                .id(id)
                .guild(guild)
                .sport(sport)
                .build());
        row.setChannelId(request.channelId());
        guildSportChannelRepository.save(row);
    }

    @Transactional
    public void upsertUserDefaultDelivery(Long userId, DiscordDeliveryRequest request) {
        validateDeliveryRequest(request);
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new ResourceNotFoundException("User not found: " + userId));

        UserDiscordDeliveryPreference preference = preferenceRepository.findById(userId)
            .orElseGet(() -> UserDiscordDeliveryPreference.builder()
                .userId(userId)
                .user(user)
                .build());
        preference.setMode(request.mode());
        preference.setGuildId(request.mode() == DiscordDeliveryMode.GUILD_CHANNEL ? request.guildId() : null);
        preferenceRepository.save(preference);
    }

    @Transactional
    public void upsertUserSportDeliveryOverride(Long userId, Long sportId, DiscordDeliveryRequest request) {
        validateDeliveryRequest(request);
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new ResourceNotFoundException("User not found: " + userId));
        Sport sport = sportRepository.findById(sportId)
            .orElseThrow(() -> new ResourceNotFoundException("Sport not found: " + sportId));

        UserDiscordSportDeliveryOverrideId id = new UserDiscordSportDeliveryOverrideId(userId, sportId);
        UserDiscordSportDeliveryOverride row = overrideRepository.findById(id)
            .orElseGet(() -> UserDiscordSportDeliveryOverride.builder()
                .id(id)
                .user(user)
                .sport(sport)
                .build());
        row.setMode(request.mode());
        row.setGuildId(request.mode() == DiscordDeliveryMode.GUILD_CHANNEL ? request.guildId() : null);
        overrideRepository.save(row);
    }

    @Transactional
    public void deleteUserSportDeliveryOverride(Long userId, Long sportId) {
        overrideRepository.deleteById(new UserDiscordSportDeliveryOverrideId(userId, sportId));
    }

    @Transactional
    public void upsertUserSportDeliveryOverrideByKey(Long userId, String sportKey, DiscordDeliveryRequest request) {
        Sport sport = sportRepository.findByKey(sportKey)
            .orElseThrow(() -> new ResourceNotFoundException("Sport not found: " + sportKey));
        upsertUserSportDeliveryOverride(userId, sport.getId(), request);
    }

    @Transactional
    public void deleteUserSportDeliveryOverrideByKey(Long userId, String sportKey) {
        Sport sport = sportRepository.findByKey(sportKey)
            .orElseThrow(() -> new ResourceNotFoundException("Sport not found: " + sportKey));
        deleteUserSportDeliveryOverride(userId, sport.getId());
    }

    private void validateDeliveryRequest(DiscordDeliveryRequest request) {
        if (request.mode() == null) {
            throw new BadRequestException("mode is required");
        }
        if (request.mode() == DiscordDeliveryMode.GUILD_CHANNEL
            && (request.guildId() == null || request.guildId().isBlank())) {
            throw new BadRequestException("guildId is required when mode=GUILD_CHANNEL");
        }
        if (request.mode() == DiscordDeliveryMode.GUILD_CHANNEL && !guildConfigRepository.existsById(request.guildId())) {
            throw new ResourceNotFoundException("Discord guild not installed: " + request.guildId());
        }
    }
}
