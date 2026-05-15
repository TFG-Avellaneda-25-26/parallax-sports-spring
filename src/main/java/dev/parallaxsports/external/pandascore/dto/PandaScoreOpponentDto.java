package dev.parallaxsports.external.pandascore.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record PandaScoreOpponentDto(
    PandaScoreTeamDto opponent,
    Integer score,
    String result
) {
}

