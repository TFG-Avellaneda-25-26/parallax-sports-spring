package dev.parallaxsports.external.pandascore.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public record PandaScoreLeagueDto(
    Long id,
    String name,
    String slug,

    @JsonAlias({"region_name", "region_code", "continent", "continent_name"})
    String region,

    @JsonAlias({"country_name", "country_code", "country_iso"})
    String country,

    @JsonProperty("image_url")
    String imageUrl
) {
}
