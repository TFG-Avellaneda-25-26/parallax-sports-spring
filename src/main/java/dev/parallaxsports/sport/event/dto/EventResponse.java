package dev.parallaxsports.sport.event.dto;

import java.time.OffsetDateTime;
import java.util.List;

public record EventResponse(
    Long id,
    String sportKey,
    String sportName,
    String eventType,
    String name,
    String status,
    OffsetDateTime startTimeUtc,
    OffsetDateTime endTimeUtc,
    String competitionName,
    String venueName,
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
