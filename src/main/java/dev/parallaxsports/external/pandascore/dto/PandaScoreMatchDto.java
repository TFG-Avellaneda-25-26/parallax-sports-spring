package dev.parallaxsports.external.pandascore.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public record PandaScoreMatchDto(
    Long id,
    String name,

    @JsonProperty("begin_at")
    String beginAt,

    @JsonProperty("end_at")
    String endAt,

    String status,
    String slug,

    @JsonProperty("league_id")
    Long leagueId,

    PandaScoreLeagueDto league,

    @JsonProperty("opponents")
    PandaScoreOpponentDto[] opponents,

    @JsonProperty("results")
    PandaScoreResultDto[] results,

    @JsonProperty("tournament")
    PandaScoreTournamentDto tournament
) {
}

