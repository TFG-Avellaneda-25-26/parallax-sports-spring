package dev.parallaxsports.external.pandascore.dto;

public record PandaScoreSyncResponse(
    String videogame,
    int matchesFetched,
    int matchesUpserted,
    java.util.Map<String, Integer> tiersFound
) {
}


