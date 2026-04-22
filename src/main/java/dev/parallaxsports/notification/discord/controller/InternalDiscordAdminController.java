package dev.parallaxsports.notification.discord.controller;

import dev.parallaxsports.notification.discord.dto.DiscordDeliveryRequest;
import dev.parallaxsports.notification.discord.dto.DiscordGuildChannelRequest;
import dev.parallaxsports.notification.discord.dto.DiscordGuildInstallRequest;
import dev.parallaxsports.notification.discord.service.DiscordAdminService;
import dev.parallaxsports.notification.service.callback.AlertCallbackAuthenticator;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Internal endpoints used by ms-discord to register guild installs, guild channel
 * choices made by server admins, and per-user delivery preferences.
 *
 * All endpoints are gated behind the shared {@code X-Api-Key} header.
 */
@RestController
@RequestMapping("/api/internal/discord")
@RequiredArgsConstructor
public class InternalDiscordAdminController {

    private final DiscordAdminService discordAdminService;
    private final AlertCallbackAuthenticator authenticator;

    @PostMapping("/guilds/{guildId}/install")
    public ResponseEntity<Void> install(
        @PathVariable String guildId,
        @RequestHeader(name = "X-Api-Key", required = false) String apiKey,
        @RequestBody DiscordGuildInstallRequest request
    ) {
        authenticator.validate(apiKey);
        discordAdminService.installGuild(guildId, request);
        return ResponseEntity.accepted().build();
    }

    @DeleteMapping("/guilds/{guildId}")
    public ResponseEntity<Void> uninstall(
        @PathVariable String guildId,
        @RequestHeader(name = "X-Api-Key", required = false) String apiKey
    ) {
        authenticator.validate(apiKey);
        discordAdminService.uninstallGuild(guildId);
        return ResponseEntity.accepted().build();
    }

    @PostMapping("/guilds/{guildId}/channel")
    public ResponseEntity<Void> upsertChannel(
        @PathVariable String guildId,
        @RequestHeader(name = "X-Api-Key", required = false) String apiKey,
        @RequestBody DiscordGuildChannelRequest request
    ) {
        authenticator.validate(apiKey);
        discordAdminService.upsertGuildChannel(guildId, request);
        return ResponseEntity.accepted().build();
    }

    @GetMapping("/users/by-discord/{discordUserId}")
    public ResponseEntity<Map<String, Long>> resolveUserId(
        @PathVariable String discordUserId,
        @RequestHeader(name = "X-Api-Key", required = false) String apiKey
    ) {
        authenticator.validate(apiKey);
        Long userId = discordAdminService.resolveUserIdByDiscordSnowflake(discordUserId);
        return ResponseEntity.ok(Map.of("userId", userId));
    }

    @PutMapping("/users/{userId}/delivery")
    public ResponseEntity<Void> upsertUserDelivery(
        @PathVariable Long userId,
        @RequestHeader(name = "X-Api-Key", required = false) String apiKey,
        @RequestBody DiscordDeliveryRequest request
    ) {
        authenticator.validate(apiKey);
        discordAdminService.upsertUserDefaultDelivery(userId, request);
        return ResponseEntity.accepted().build();
    }

    @PutMapping("/users/{userId}/delivery/sports/{sportId}")
    public ResponseEntity<Void> upsertUserSportDelivery(
        @PathVariable Long userId,
        @PathVariable Long sportId,
        @RequestHeader(name = "X-Api-Key", required = false) String apiKey,
        @RequestBody DiscordDeliveryRequest request
    ) {
        authenticator.validate(apiKey);
        discordAdminService.upsertUserSportDeliveryOverride(userId, sportId, request);
        return ResponseEntity.accepted().build();
    }

    @DeleteMapping("/users/{userId}/delivery/sports/{sportId}")
    public ResponseEntity<Void> deleteUserSportDelivery(
        @PathVariable Long userId,
        @PathVariable Long sportId,
        @RequestHeader(name = "X-Api-Key", required = false) String apiKey
    ) {
        authenticator.validate(apiKey);
        discordAdminService.deleteUserSportDeliveryOverride(userId, sportId);
        return ResponseEntity.accepted().build();
    }

    @PutMapping("/users/{userId}/delivery/sport-keys/{sportKey}")
    public ResponseEntity<Void> upsertUserSportDeliveryByKey(
        @PathVariable Long userId,
        @PathVariable String sportKey,
        @RequestHeader(name = "X-Api-Key", required = false) String apiKey,
        @RequestBody DiscordDeliveryRequest request
    ) {
        authenticator.validate(apiKey);
        discordAdminService.upsertUserSportDeliveryOverrideByKey(userId, sportKey, request);
        return ResponseEntity.accepted().build();
    }

    @DeleteMapping("/users/{userId}/delivery/sport-keys/{sportKey}")
    public ResponseEntity<Void> deleteUserSportDeliveryByKey(
        @PathVariable Long userId,
        @PathVariable String sportKey,
        @RequestHeader(name = "X-Api-Key", required = false) String apiKey
    ) {
        authenticator.validate(apiKey);
        discordAdminService.deleteUserSportDeliveryOverrideByKey(userId, sportKey);
        return ResponseEntity.accepted().build();
    }
}
