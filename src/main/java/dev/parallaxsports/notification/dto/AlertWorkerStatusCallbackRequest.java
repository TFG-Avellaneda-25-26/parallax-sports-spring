package dev.parallaxsports.notification.dto;

/**
 * Status callback payload sent by the notification microservice for delivery
 * lifecycle updates.
 *
 * The callback includes transition target status, notification microservice
 * instance metadata, provider metadata, and optional failure diagnostics used
 * for retry/permanent-failure decisions.
 */
public record AlertWorkerStatusCallbackRequest(
    String status,
    String workerId,
    String streamMessageId,
    String providerMessageId,
    String errorCode,
    String errorMessage,
    Integer httpStatus,
    Integer latencyMs
) {
}
