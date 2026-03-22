package dev.parallaxsports.basketball.dto;

import java.time.OffsetDateTime;

public record BasketballGameResponse(
    Long eventId,
    Long externalGameId,
    OffsetDateTime startTimeUtc,
    String status,
    Long leagueId,
    String leagueName,
    String season,
    Long homeTeamId,
    String homeTeam,
    String homeTeamLogo,
    Integer homeScore,
    Long awayTeamId,
    String awayTeam,
    String awayTeamLogo,
    Integer awayScore,
    String venue
) {
}
