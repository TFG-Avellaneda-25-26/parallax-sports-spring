package dev.parallaxsports.formula1.dto;

import java.time.OffsetDateTime;

public record Formula1SessionResponse(
    Long eventId,
    String circuitName,
    String grandPrixName,
    String circuitLogoUrl,
    OffsetDateTime startTimeUtc,
    OffsetDateTime endTimeUtc,
    String eventType,
    String sessionName
) {
}
 