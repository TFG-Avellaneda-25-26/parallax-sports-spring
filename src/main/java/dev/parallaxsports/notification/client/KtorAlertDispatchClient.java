package dev.parallaxsports.notification.client;

import dev.parallaxsports.core.config.properties.AlertProperties;
import dev.parallaxsports.notification.model.UserEventAlert;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
@RequiredArgsConstructor
public class KtorAlertDispatchClient {

    private final RestClient.Builder restClientBuilder;
    private final AlertProperties alertProperties;

    public void dispatch(UserEventAlert alert) {
        String baseUrl = alertProperties.getKtorBaseUrl();
        if (baseUrl == null || baseUrl.isBlank()) {
            throw new IllegalStateException("app.alerts.ktor-base-url must be configured for dispatch");
        }

        RestClient restClient = restClientBuilder.baseUrl(baseUrl).build();
        AlertDispatchRequest payload = new AlertDispatchRequest(
            alert.getId(),
            alert.getUser().getId(),
            alert.getEventId(),
            alert.getChannel(),
            alert.getLeadTimeMinutes(),
            alert.getSendAtUtc()
        );

        RestClient.RequestBodySpec request = restClient.post()
            .uri(alertProperties.getKtorDispatchPath())
            .contentType(MediaType.APPLICATION_JSON);

        String apiKey = alertProperties.getKtorApiKey();
        if (apiKey != null && !apiKey.isBlank()) {
            request = request.header("X-Api-Key", apiKey);
        }

        request.body(payload).retrieve().toBodilessEntity();
    }

    private record AlertDispatchRequest(
        Long alertId,
        Long userId,
        Long eventId,
        String channel,
        Integer leadTimeMinutes,
        java.time.OffsetDateTime sendAtUtc
    ) {
    }
}
