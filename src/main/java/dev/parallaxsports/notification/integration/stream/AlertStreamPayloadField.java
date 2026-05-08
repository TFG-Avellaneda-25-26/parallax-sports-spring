package dev.parallaxsports.notification.integration.stream;

import java.util.Map;

/**
 * Centralized key registry for Redis alert stream payload fields.
 *
 * This enum keeps key naming stable across publisher and consumer contracts.
 */
enum AlertStreamPayloadField {
    SCHEMA_VERSION("schemaVersion"),
    ALERT_ID("alertId"),
    USER_ID("userId"),
    EVENT_ID("eventId"),
    CHANNEL("channel"),
    SEND_AT_UTC("sendAtUtc"),
    IDEMPOTENCY_KEY("idempotencyKey"),
    ATTEMPTS("attempts"),
    MAX_ATTEMPTS("maxAttempts"),
    ARTIFACT_REQUIRED("artifactRequired"),
    ARTIFACT_ID("artifactId"),
    EVENT_NAME("eventName"),
    EVENT_TYPE("eventType"),
    EVENT_STATUS("eventStatus"),
    EVENT_START_TIME_UTC("eventStartTimeUtc"),
    EVENT_END_TIME_UTC("eventEndTimeUtc"),
    COMPETITION_NAME("competitionName"),
    VENUE_NAME("venueName"),
    VENUE_TIMEZONE("venueTimezone"),
    USER_TIMEZONE("userTimezone"),
    USER_LOCALE("userLocale"),
    USER_EMAIL("userEmail"),
    RENDER_HASH("renderHash"),
    DISCORD_DELIVERY_MODE("discordDeliveryMode"),
    DISCORD_USER_ID("discordUserId"),
    DISCORD_CHANNEL_ID("discordChannelId"),
    DISCORD_GUILD_ID("discordGuildId");

    private final String key;

    AlertStreamPayloadField(String key) {
        this.key = key;
    }

    /**
     * Writes a non-null string value under this field key.
     *
     * @param payload target payload map
     * @param value field value
     */
    void put(Map<String, String> payload, String value) {
        if (value != null) {
            payload.put(key, value);
        }
    }

    /**
     * Writes a non-null numeric value under this field key.
     *
     * @param payload target payload map
     * @param value field value
     */
    void put(Map<String, String> payload, Number value) {
        if (value != null) {
            payload.put(key, String.valueOf(value));
        }
    }

    /**
     * Writes a non-null boolean value under this field key.
     *
     * @param payload target payload map
     * @param value field value
     */
    void put(Map<String, String> payload, Boolean value) {
        if (value != null) {
            payload.put(key, String.valueOf(value));
        }
    }
}