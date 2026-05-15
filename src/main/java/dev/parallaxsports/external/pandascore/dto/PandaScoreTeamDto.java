package dev.parallaxsports.external.pandascore.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public record PandaScoreTeamDto(
    Long id,
    String name,
    String acronym,
    String slug,

    @JsonProperty("image_url")
    String imageUrl
) {
}

