package dev.parallaxsports.pandascore.dto;

import java.time.OffsetDateTime;

public record PandaScoreMatchResponse(
    Long id,
    String name,
    String leagueName,
    String status,
    String slug,
    OffsetDateTime beginAt,
    OffsetDateTime endAt,
    String videogame
) {
}

