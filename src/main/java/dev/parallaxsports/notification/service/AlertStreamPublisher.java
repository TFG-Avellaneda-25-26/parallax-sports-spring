package dev.parallaxsports.notification.service;

import dev.parallaxsports.notification.model.UserEventAlert;
import java.util.HashMap;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.data.redis.connection.stream.RecordId;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class AlertStreamPublisher {

    private final StringRedisTemplate stringRedisTemplate;

    public String publish(String streamName, UserEventAlert alert) {
        Map<String, String> payload = new HashMap<>();
        payload.put("alertId", String.valueOf(alert.getId()));
        payload.put("userId", String.valueOf(alert.getUser().getId()));
        payload.put("eventId", String.valueOf(alert.getEventId()));
        payload.put("channel", alert.getChannel());
        payload.put("sendAtUtc", String.valueOf(alert.getSendAtUtc()));
        payload.put("idempotencyKey", alert.getIdempotencyKey());
        payload.put("attempts", String.valueOf(alert.getAttempts()));
        payload.put("maxAttempts", String.valueOf(alert.getMaxAttempts()));
        payload.put("artifactRequired", String.valueOf(alert.isArtifactRequired()));
        if (alert.getArtifactId() != null) {
            payload.put("artifactId", String.valueOf(alert.getArtifactId()));
        }

        RecordId recordId = stringRedisTemplate.opsForStream().add(MapRecord.create(streamName, payload));
        if (recordId == null) {
            throw new IllegalStateException("Redis stream publish returned null record id");
        }
        return recordId.getValue();
    }
}
