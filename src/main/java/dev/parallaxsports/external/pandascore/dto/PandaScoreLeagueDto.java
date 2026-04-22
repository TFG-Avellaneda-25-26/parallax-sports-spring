package dev.parallaxsports.external.pandascore.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public record PandaScoreLeagueDto(
    Long id,
    String name,
    String slug,

    String region,

    String country,

    @JsonProperty("image_url")
    String imageUrl
) {
}

