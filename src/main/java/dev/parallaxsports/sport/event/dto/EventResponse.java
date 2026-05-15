package dev.parallaxsports.sport.event.dto;

import java.time.OffsetDateTime;
import java.util.List;

public record EventResponse(
    Long id,
    Long parentEventId,
    String sportKey,
    String sportName,
    String eventType,
    String name,
    String status,
    OffsetDateTime startTimeUtc,
    OffsetDateTime endTimeUtc,
    String competitionName,
    String venueName,
    String sportIconUrl,
    String competitionLogoUrl,
    String venueImageUrl,
    List<ParticipantEntry> participants,
    List<String> logos
) {

    public record ParticipantEntry(
        Long id,
        String name,
        String shortName,
        String side
    ) {}
}
