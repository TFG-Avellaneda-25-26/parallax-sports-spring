package dev.parallaxsports.pandascore.dto;

public record PandaScoreSyncResponse(
    String videogame,
    int matchesFetched,
    int matchesUpserted
) {
}

