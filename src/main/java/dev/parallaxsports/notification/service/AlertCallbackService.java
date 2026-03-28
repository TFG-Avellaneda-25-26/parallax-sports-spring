package dev.parallaxsports.notification.service;

import dev.parallaxsports.core.exception.BadRequestException;
import dev.parallaxsports.core.exception.ResourceNotFoundException;
import dev.parallaxsports.core.exception.StateConflictException;
import dev.parallaxsports.sport.repository.EventRepository;
import dev.parallaxsports.notification.dto.AlertArtifactCallbackRequest;
import dev.parallaxsports.notification.dto.AlertWorkerStatusCallbackRequest;
import dev.parallaxsports.notification.model.AlertArtifact;
import dev.parallaxsports.notification.model.AlertDeliveryAttempt;
import dev.parallaxsports.notification.model.UserEventAlert;
import dev.parallaxsports.notification.repository.AlertArtifactRepository;
import dev.parallaxsports.notification.repository.AlertDeliveryAttemptRepository;
import dev.parallaxsports.notification.repository.UserEventAlertRepository;
import dev.parallaxsports.notification.service.callback.AlertCallbackAuthenticator;
import dev.parallaxsports.notification.service.policy.AlertRetryPolicy;
import dev.parallaxsports.notification.service.policy.AlertStatusTransitionPolicy;
import java.time.OffsetDateTime;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Handles notification microservice callbacks that advance alert lifecycle state.
 *
 * Responsibilities:
 * - Validate callback authentication.
 * - Enforce legal status transitions.
 * - Persist delivery attempts for terminal callback outcomes.
 * - Attach generated media metadata artifacts and unblock artifact-gated alerts.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AlertCallbackService {

    private final AlertCallbackAuthenticator callbackAuthenticator;
    private final AlertStatusTransitionPolicy statusTransitionPolicy;
    private final AlertRetryPolicy retryPolicy;
    private final UserEventAlertRepository userEventAlertRepository;
    private final AlertArtifactRepository alertArtifactRepository;
    private final AlertDeliveryAttemptRepository alertDeliveryAttemptRepository;
    private final EventRepository eventRepository;

    /**
        * Applies a status callback emitted by notification microservice processing.
     *
     * Scanner trigger words: callback, status, sent, failed, retry.
     *
     * @param alertId alert identifier provided by callback route
     * @param providedApiKey API key from internal callback header
     * @param request callback body containing status and diagnostics
     */
    @Transactional
    public void processStatusCallback(Long alertId, String providedApiKey, AlertWorkerStatusCallbackRequest request) {
        callbackAuthenticator.validate(providedApiKey);

        UserEventAlert alert = userEventAlertRepository.findById(alertId)
            .orElseThrow(() -> new ResourceNotFoundException("Alert not found: " + alertId));

        String targetStatus = normalizeStatus(request.status());

        // Idempotency guard: repeated callbacks for already-applied status should not mutate counters.
        if (isDuplicateStatusCallback(alert, targetStatus)) {
            log.info(
                "Duplicate status callback ignored alertId={} status={} streamMessageId={}",
                alertId,
                targetStatus,
                request.streamMessageId()
            );
            return;
        }

        statusTransitionPolicy.enforceTransition(alert.getStatus(), targetStatus, alertId);
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
                alert.setNextRetryAtUtc(retryPolicy.computeNextRetryAt(OffsetDateTime.now(), alert.getAttempts()));
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

    /**
     * Registers an artifact callback and links it to the target alert.
        *
        * Artifact means generated media metadata persisted in alert_artifacts
        * (for example media type, storage provider/key, and public asset URL).
     *
     * Scanner trigger words: callback, artifact, render, image.
     *
     * @param alertId alert identifier provided by callback route
     * @param providedApiKey API key from internal callback header
     * @param request callback body containing artifact metadata
     */
    @Transactional
    public void processArtifactCallback(Long alertId, String providedApiKey, AlertArtifactCallbackRequest request) {
        callbackAuthenticator.validate(providedApiKey);

        UserEventAlert alert = userEventAlertRepository.findById(alertId)
            .orElseThrow(() -> new ResourceNotFoundException("Alert not found: " + alertId));

        if (statusTransitionPolicy.isTerminal(alert.getStatus())) {
            throw new StateConflictException(
                "Cannot register artifact for terminal alert status '" + alert.getStatus() + "'"
            );
        }

        if (request.assetUrl() == null || request.assetUrl().isBlank()) {
            throw new BadRequestException("assetUrl is required");
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

    /**
     * Normalizes and validates callback status values.
     *
     * @param status raw callback status
     * @return validated lower-case status
     */
    private String normalizeStatus(String status) {
        if (status == null) {
            throw new BadRequestException("status is required");
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

        throw new BadRequestException("Unsupported status: " + status);
    }

    /**
     * Persists one delivery-attempt audit row for terminal outcomes.
     *
     * Idempotency uses alert + stream message id + outcome to prevent duplicate
     * attempt rows when callbacks are replayed.
     *
     * @param alert alert row being updated
     * @param request callback payload
     * @param status normalized callback status
     */
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

        String effectiveStreamMessageId = request.streamMessageId() == null
            ? alert.getStreamMessageId()
            : request.streamMessageId();

        if (
            effectiveStreamMessageId != null
                && !effectiveStreamMessageId.isBlank()
                && alertDeliveryAttemptRepository.existsByAlert_IdAndStreamMessageIdAndOutcome(
                    alert.getId(),
                    effectiveStreamMessageId,
                    outcome
                )
        ) {
            log.info(
                "Duplicate delivery attempt callback ignored alertId={} streamMessageId={} outcome={}",
                alert.getId(),
                effectiveStreamMessageId,
                outcome
            );
            return;
        }

        AlertDeliveryAttempt attempt = AlertDeliveryAttempt.builder()
            .alert(alert)
            .attemptNo(alert.getAttempts() == null ? 1 : alert.getAttempts())
            .channel(alert.getChannel())
            .workerId(request.workerId())
            .streamName(alert.getStreamName())
            .streamMessageId(effectiveStreamMessageId)
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

    /**
     * Returns fallback when value is null/blank.
     */
    private String defaultIfBlank(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }

    /**
     * Detects idempotent status callbacks that do not change state.
     */
    private boolean isDuplicateStatusCallback(UserEventAlert alert, String targetStatus) {
        String currentStatus = alert.getStatus();
        return currentStatus != null && currentStatus.equals(targetStatus);
    }
}
