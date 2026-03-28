package dev.parallaxsports.external.basketball.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public record BalldontlieGameDto(
    Long id,
    String date,
    Integer season,
    String status,
    Integer period,
    String time,
    Boolean postseason,
    Boolean postponed,
    @JsonProperty("home_team_score") Integer homeTeamScore,
    @JsonProperty("visitor_team_score") Integer visitorTeamScore,
    String datetime,
    @JsonProperty("home_team") TeamDto homeTeam,
    @JsonProperty("visitor_team") TeamDto visitorTeam
) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record TeamDto(
        Long id,
        String conference,
        String division,
        String city,
        String name,
        @JsonProperty("full_name") String fullName,
        String abbreviation
    ) {
    }
}