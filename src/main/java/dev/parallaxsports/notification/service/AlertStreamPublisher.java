package dev.parallaxsports.notification.service;

import dev.parallaxsports.core.config.properties.AlertProperties;
import dev.parallaxsports.core.exception.ServiceUnavailableException;
import dev.parallaxsports.formula1.model.Event;
import dev.parallaxsports.notification.integration.stream.AlertStreamPayloadBuilder;
import dev.parallaxsports.notification.model.UserEventAlert;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.data.redis.connection.stream.RecordId;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

/**
 * Publishes claimed alerts to Redis streams.
 *
 * This class owns transport concerns (publish + trim + transport error mapping)
 * while payload assembly is delegated to {@link AlertStreamPayloadBuilder}.
 */
@Component
@RequiredArgsConstructor
public class AlertStreamPublisher {

    private final StringRedisTemplate stringRedisTemplate;
    private final AlertProperties alertProperties;
    private final AlertStreamPayloadBuilder payloadBuilder;

    /**
     * Publishes one alert message to a channel stream.
     *
     * @param streamName destination stream name
     * @param alert claimed alert row
     * @param event optional event enrichment entity
     * @return Redis stream message id
     */
    public String publish(String streamName, UserEventAlert alert, Event event) {
        var payload = payloadBuilder.build(alert, event);

        try {
            RecordId recordId = stringRedisTemplate.opsForStream().add(MapRecord.create(streamName, payload));
            if (recordId == null) {
                throw new ServiceUnavailableException("Redis stream publish returned null record id");
            }
            trimStream(streamName);
            return recordId.getValue();
        } catch (RuntimeException ex) {
            throw new ServiceUnavailableException("Failed to publish alert to Redis stream", ex);
        }
    }

    /**
     * Trims stream length when retention limits are enabled.
     *
     * @param streamName stream to trim
     */
    private void trimStream(String streamName) {
        if (!alertProperties.isStreamTrimEnabled()) {
            return;
        }

        long maxLen = Math.max(alertProperties.getStreamMaxLen(), 1L);
        stringRedisTemplate.opsForStream().trim(streamName, maxLen);
    }
}
