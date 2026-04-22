package dev.parallaxsports.notification.integration.stream;

import dev.parallaxsports.notification.discord.service.DiscordRouting;
import dev.parallaxsports.sport.model.Event;
import dev.parallaxsports.notification.model.UserEventAlert;
import dev.parallaxsports.user.model.User;
import dev.parallaxsports.user.model.UserSettings;
import java.util.HashMap;
import java.util.Map;
import org.springframework.stereotype.Component;

/**
 * Builds outbound Redis stream payloads for alert dispatch.
 */
@Component
public class AlertStreamPayloadBuilder {

    private static final String PAYLOAD_SCHEMA_VERSION = "v1";

    public Map<String, String> build(UserEventAlert alert, Event event, User user, String renderHash) {
        return build(alert, event, user, renderHash, null);
    }

    public Map<String, String> build(
        UserEventAlert alert,
        Event event,
        User user,
        String renderHash,
        DiscordRouting discordRouting
    ) {
        Map<String, String> payload = new HashMap<>();
        appendBasePayload(payload, alert);
        appendEventPayload(payload, event);
        appendUserPayload(payload, user);
        AlertStreamPayloadField.RENDER_HASH.put(payload, renderHash);
        appendDiscordRouting(payload, discordRouting);
        return payload;
    }

    private void appendDiscordRouting(Map<String, String> payload, DiscordRouting routing) {
        if (routing == null || !routing.isRoutable()) {
            return;
        }
        AlertStreamPayloadField.DISCORD_DELIVERY_MODE.put(payload, routing.mode().name());
        AlertStreamPayloadField.DISCORD_USER_ID.put(payload, routing.discordUserId());
        AlertStreamPayloadField.DISCORD_CHANNEL_ID.put(payload, routing.discordChannelId());
        AlertStreamPayloadField.DISCORD_GUILD_ID.put(payload, routing.discordGuildId());
    }

    private void appendBasePayload(Map<String, String> payload, UserEventAlert alert) {
        AlertStreamPayloadField.SCHEMA_VERSION.put(payload, PAYLOAD_SCHEMA_VERSION);
        AlertStreamPayloadField.ALERT_ID.put(payload, alert.getId());
        AlertStreamPayloadField.USER_ID.put(payload, alert.getUserId());
        AlertStreamPayloadField.EVENT_ID.put(payload, alert.getEventId());
        AlertStreamPayloadField.CHANNEL.put(payload, alert.getChannel());
        AlertStreamPayloadField.SEND_AT_UTC.put(payload, String.valueOf(alert.getSendAtUtc()));
        AlertStreamPayloadField.IDEMPOTENCY_KEY.put(payload, alert.getIdempotencyKey());
        AlertStreamPayloadField.ATTEMPTS.put(payload, alert.getAttempts());
        AlertStreamPayloadField.MAX_ATTEMPTS.put(payload, alert.getMaxAttempts());
        AlertStreamPayloadField.ARTIFACT_REQUIRED.put(payload, alert.isArtifactRequired());
        AlertStreamPayloadField.ARTIFACT_ID.put(payload, alert.getArtifactId());
    }

    private void appendEventPayload(Map<String, String> payload, Event event) {
        if (event == null) {
            return;
        }
        AlertStreamPayloadField.EVENT_NAME.put(payload, event.getName());
        AlertStreamPayloadField.EVENT_TYPE.put(payload, event.getEventType());
        AlertStreamPayloadField.EVENT_STATUS.put(payload, event.getStatus());
        AlertStreamPayloadField.EVENT_START_TIME_UTC.put(payload, String.valueOf(event.getStartTimeUtc()));
        if (event.getEndTimeUtc() != null) {
            AlertStreamPayloadField.EVENT_END_TIME_UTC.put(payload, String.valueOf(event.getEndTimeUtc()));
        }
        if (event.getCompetition() != null) {
            AlertStreamPayloadField.COMPETITION_NAME.put(payload, event.getCompetition().getName());
        }
        if (event.getVenue() != null) {
            AlertStreamPayloadField.VENUE_NAME.put(payload, event.getVenue().getName());
            AlertStreamPayloadField.VENUE_TIMEZONE.put(payload, event.getVenue().getTimezone());
        }
    }

    private void appendUserPayload(Map<String, String> payload, User user) {
        if (user == null) return;
        AlertStreamPayloadField.USER_EMAIL.put(payload, user.getEmail());
        UserSettings settings = user.getSettings();
        if (settings != null) {
            AlertStreamPayloadField.USER_TIMEZONE.put(payload, settings.getTimezone());
            AlertStreamPayloadField.USER_LOCALE.put(payload, settings.getLocale());
        }
    }
}
