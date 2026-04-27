package dev.parallaxsports.external.pandascore.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record PandaScoreTournamentDto(
    Long id,
    String name,
    String tier
) {
}
