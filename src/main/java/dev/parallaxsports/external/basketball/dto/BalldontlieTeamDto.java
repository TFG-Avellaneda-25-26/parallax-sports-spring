package dev.parallaxsports.external.basketball.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public record BalldontlieTeamDto(
    Long id,
    String conference,
    String division,
    String city,
    String name,
    @JsonProperty("full_name") String fullName,
    String abbreviation
) {
}