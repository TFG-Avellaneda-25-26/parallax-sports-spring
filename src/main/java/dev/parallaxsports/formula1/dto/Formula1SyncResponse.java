package dev.parallaxsports.formula1.dto;

public record Formula1SyncResponse(
    int year,
    int meetingsFetched,
    int sessionsFetched,
    int meetingsUpserted,
    int sessionsUpserted,
    int venuesUpserted
) {
}
