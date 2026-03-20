package dev.parallaxsports.notification.dto;

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
