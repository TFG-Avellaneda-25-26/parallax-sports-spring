package dev.parallaxsports.notification.service;

import dev.parallaxsports.core.config.properties.AlertProperties;
import dev.parallaxsports.formula1.repository.EventRepository;
import dev.parallaxsports.notification.dto.AlertArtifactCallbackRequest;
import dev.parallaxsports.notification.dto.AlertWorkerStatusCallbackRequest;
import dev.parallaxsports.notification.model.AlertArtifact;
import dev.parallaxsports.notification.model.AlertDeliveryAttempt;
import dev.parallaxsports.notification.model.UserEventAlert;
import dev.parallaxsports.notification.repository.AlertArtifactRepository;
import dev.parallaxsports.notification.repository.AlertDeliveryAttemptRepository;
import dev.parallaxsports.notification.repository.UserEventAlertRepository;
import java.time.OffsetDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class AlertCallbackService {

    private static final String X_API_KEY = "X-Api-Key";

    private final AlertProperties alertProperties;
    private final UserEventAlertRepository userEventAlertRepository;
    private final AlertArtifactRepository alertArtifactRepository;
    private final AlertDeliveryAttemptRepository alertDeliveryAttemptRepository;
    private final EventRepository eventRepository;

    @Transactional
    public void processStatusCallback(Long alertId, String providedApiKey, AlertWorkerStatusCallbackRequest request) {
        requireValidApiKey(providedApiKey);

        UserEventAlert alert = userEventAlertRepository.findById(alertId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Alert not found: " + alertId));

        String targetStatus = normalizeStatus(request.status());
        alert.setWorkerId(request.workerId());
        alert.setProviderMessageId(request.providerMessageId());
        if (request.streamMessageId() != null && !request.streamMessageId().isBlank()) {
            alert.setStreamMessageId(request.streamMessageId());
        }

        if ("processing".equals(targetStatus)) {
            alert.setStatus("processing");
            alert.setProcessingStartedAtUtc(OffsetDateTime.now());
            userEventAlertRepository.save(alert);
            return;
        }

        int nextAttempt = (alert.getAttempts() == null ? 0 : alert.getAttempts()) + 1;
        if ("sent".equals(targetStatus)) {
            alert.setStatus("sent");
            alert.setSentAtUtc(OffsetDateTime.now());
            alert.setAttempts(nextAttempt);
            alert.setLastError(null);
            alert.setLastErrorCode(null);
        } else if ("failed_retryable".equals(targetStatus)) {
            alert.setAttempts(nextAttempt);
            if (alert.getAttempts() >= alert.getMaxAttempts()) {
                alert.setStatus("failed_permanent");
            } else {
                alert.setStatus("failed_retryable");
                alert.setNextRetryAtUtc(computeNextRetryAt(OffsetDateTime.now(), alert.getAttempts()));
            }
            alert.setLastError(request.errorMessage());
            alert.setLastErrorCode(request.errorCode());
        } else if ("failed_permanent".equals(targetStatus)) {
            alert.setAttempts(nextAttempt);
            alert.setStatus("failed_permanent");
            alert.setLastError(request.errorMessage());
            alert.setLastErrorCode(request.errorCode());
        } else if ("cancelled".equals(targetStatus)) {
            alert.setStatus("cancelled");
            alert.setLastError(request.errorMessage());
            alert.setLastErrorCode(request.errorCode());
        }

        userEventAlertRepository.save(alert);
        saveAttempt(alert, request, targetStatus);
    }

    @Transactional
    public void processArtifactCallback(Long alertId, String providedApiKey, AlertArtifactCallbackRequest request) {
        requireValidApiKey(providedApiKey);

        UserEventAlert alert = userEventAlertRepository.findById(alertId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Alert not found: " + alertId));

        if (request.assetUrl() == null || request.assetUrl().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "assetUrl is required");
        }

        AlertArtifact artifact = AlertArtifact.builder()
            .event(eventRepository.getReferenceById(alert.getEventId()))
            .artifactType(defaultIfBlank(request.artifactType(), "image"))
            .storageProvider(defaultIfBlank(request.storageProvider(), "cloudinary"))
            .storageKey(request.storageKey())
            .assetUrl(request.assetUrl())
            .renderContextHash(defaultIfBlank(request.renderContextHash(), "default"))
            .build();

        AlertArtifact savedArtifact = alertArtifactRepository.save(artifact);
        alert.setArtifactId(savedArtifact.getId());

        if (alert.isArtifactRequired() && "waiting_artifact".equals(alert.getStatus())) {
            alert.setStatus("scheduled");
        }
        userEventAlertRepository.save(alert);
    }

    private void requireValidApiKey(String providedApiKey) {
        String expectedApiKey = alertProperties.getKtorApiKey();
        if (expectedApiKey == null || expectedApiKey.isBlank()) {
            throw new ResponseStatusException(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "Missing server callback key configuration"
            );
        }
        if (providedApiKey == null || !expectedApiKey.equals(providedApiKey)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid " + X_API_KEY);
        }
    }

    private String normalizeStatus(String status) {
        if (status == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "status is required");
        }

        String normalized = status.toLowerCase();
        if (
            "processing".equals(normalized)
                || "sent".equals(normalized)
                || "failed_retryable".equals(normalized)
                || "failed_permanent".equals(normalized)
                || "cancelled".equals(normalized)
        ) {
            return normalized;
        }

        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unsupported status: " + status);
    }

    private void saveAttempt(UserEventAlert alert, AlertWorkerStatusCallbackRequest request, String status) {
        String outcome = switch (status) {
            case "sent" -> "success";
            case "failed_retryable" -> "retryable_failure";
            case "failed_permanent", "cancelled" -> "permanent_failure";
            default -> null;
        };

        if (outcome == null) {
            return;
        }

        AlertDeliveryAttempt attempt = AlertDeliveryAttempt.builder()
            .alert(alert)
            .attemptNo(alert.getAttempts() == null ? 1 : alert.getAttempts())
            .channel(alert.getChannel())
            .workerId(request.workerId())
            .streamName(alert.getStreamName())
            .streamMessageId(request.streamMessageId() == null ? alert.getStreamMessageId() : request.streamMessageId())
            .finishedAt(OffsetDateTime.now())
            .outcome(outcome)
            .errorCode(request.errorCode())
            .errorMessage(request.errorMessage())
            .httpStatus(request.httpStatus())
            .providerMessageId(request.providerMessageId())
            .latencyMs(request.latencyMs())
            .build();

        alertDeliveryAttemptRepository.save(attempt);
    }

    private OffsetDateTime computeNextRetryAt(OffsetDateTime now, Integer attempts) {
        int currentAttempts = attempts == null ? 1 : attempts;
        int[] scheduleMinutes = {1, 3, 10, 30, 120, 480};
        int index = Math.min(Math.max(currentAttempts - 1, 0), scheduleMinutes.length - 1);
        return now.plusMinutes(scheduleMinutes[index]);
    }

    private String defaultIfBlank(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }
}
