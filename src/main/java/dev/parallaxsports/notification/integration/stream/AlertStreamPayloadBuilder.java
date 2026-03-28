package dev.parallaxsports.notification.integration.stream;

import dev.parallaxsports.sport.model.Event;
import dev.parallaxsports.notification.model.UserEventAlert;
import java.util.HashMap;
import java.util.Map;
import org.springframework.stereotype.Component;

/**
 * Builds outbound Redis stream payloads for alert dispatch.
 *
 * Payload composition is intentionally split into stable dispatch identity fields
 * and optional event-enrichment fields.
 */
@Component
public class AlertStreamPayloadBuilder {

    private static final String PAYLOAD_SCHEMA_VERSION = "v1";

        /**
         * Builds the full payload map consumed by the notification microservice.
         *
         * @param alert claimed alert lifecycle row
         * @param event optional event entity for enrichment fields
         * @return string payload map ready for Redis stream publish
         */
    public Map<String, String> build(UserEventAlert alert, Event event) {
        Map<String, String> payload = new HashMap<>();
        appendBasePayload(payload, alert);
        appendEventPayload(payload, event);
        return payload;
    }

        /*============================================================
            PAYLOAD SEGMENTS
            Required identity/routing fields and optional event enrichment fields
        ============================================================*/

        /**
         * Appends base alert fields that are required for dispatch semantics.
         *
         * This section contains the invariant contract: routing, retry, idempotency,
         * and artifact-gating values that the notification microservice needs
         * even when event enrichment is absent.

         * Artifact means generated media metadata attached to an alert
         * (for example media id/url) before dispatch.
         *
         * @param payload target payload map
         * @param alert claimed alert lifecycle row
         */
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

    /**
        * Appends optional event context fields used for richer notification-side rendering.
     *
     * Unlike {@link #appendBasePayload(Map, UserEventAlert)}, this section is best-effort
     * enrichment and may be partially or fully omitted without breaking core delivery flow.
     *
     * @param payload target payload map
    * @param event optional event entity to enrich notification microservice context
     */
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
}