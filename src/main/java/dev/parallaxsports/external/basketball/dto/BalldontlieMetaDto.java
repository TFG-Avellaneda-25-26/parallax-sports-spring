package dev.parallaxsports.external.basketball.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record BalldontlieMetaDto(
    Long next_cursor,
    Integer per_page
) {
}