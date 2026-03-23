package dev.parallaxsports.sport.basketball.dto;

public record BasketballTeamResponse(
    Long id,
    String name,
    String fullName,
    String abbreviation,
    String conference,
    String division,
    String city,
    String logoUrl
) {
}