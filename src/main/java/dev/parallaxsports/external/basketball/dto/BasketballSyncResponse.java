package dev.parallaxsports.external.basketball.dto;

import java.time.LocalDate;

public record BasketballSyncResponse(
    LocalDate fromDate,
    LocalDate toDate,
    int maxPages,
    int pagesUsed,
    int datesSynced,
    int gamesFetched,
    int gamesUpserted,
    LocalDate nextCursorDate,
    boolean incomplete
) {
}
