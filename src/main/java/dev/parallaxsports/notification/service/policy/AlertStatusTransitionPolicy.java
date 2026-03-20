package dev.parallaxsports.notification.service.policy;

import dev.parallaxsports.core.exception.StateConflictException;
import org.springframework.stereotype.Component;

/**
 * Enforces valid alert lifecycle transitions and terminal-state guards.
 */
@Component
public class AlertStatusTransitionPolicy {

    /**
     * Validates a requested lifecycle transition against allowed state rules.
     *
     * @param currentStatus current persisted alert status
     * @param targetStatus requested target status
     * @param alertId alert identifier used in conflict messages
     */
    public void enforceTransition(String currentStatus, String targetStatus, Long alertId) {
        if (currentStatus == null || currentStatus.isBlank()) {
            throw new StateConflictException("Alert " + alertId + " has no current status");
        }

        if (currentStatus.equals(targetStatus)) {
            return;
        }

        if (isTerminal(currentStatus)) {
            throw new StateConflictException(
                "Alert " + alertId + " is already in terminal status '" + currentStatus + "'"
            );
        }

        boolean valid = switch (targetStatus) {
            case "processing" -> "queued".equals(currentStatus) || "processing".equals(currentStatus);
            case "sent", "failed_retryable", "failed_permanent", "cancelled" ->
                "queued".equals(currentStatus) || "processing".equals(currentStatus);
            default -> false;
        };

        if (!valid) {
            throw new StateConflictException(
                "Invalid alert status transition for alert " + alertId
                    + ": " + currentStatus + " -> " + targetStatus
            );
        }
    }

    /**
     * Indicates whether a status is terminal and immutable for further transitions.
     *
     * @param status alert status value
     * @return true when status is terminal
     */
    public boolean isTerminal(String status) {
        return "sent".equals(status)
            || "failed_permanent".equals(status)
            || "cancelled".equals(status);
    }
}