package dev.parallaxsports.sport.basketball.dto;

import java.time.OffsetDateTime;

public record BasketballGameResponse(
    Long eventId,
    OffsetDateTime startTimeUtc,
    String status,
    String eventType,
    Long competitionId,
    String competitionName,
    Long homeTeamId,
    String homeTeam,
    String homeTeamLogo,
    Long awayTeamId,
    String awayTeam,
    String awayTeamLogo
) {
}
