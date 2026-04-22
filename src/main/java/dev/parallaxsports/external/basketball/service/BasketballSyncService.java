package dev.parallaxsports.external.basketball.service;

import dev.parallaxsports.sport.basketball.BasketballLeague;
import dev.parallaxsports.sport.basketball.dto.BasketballGameResponse;
import dev.parallaxsports.sport.basketball.dto.BasketballTeamResponse;
import dev.parallaxsports.core.exception.BadRequestException;
import dev.parallaxsports.core.exception.UpstreamServiceException;
import dev.parallaxsports.external.basketball.client.BalldontlieBasketballClient;
import dev.parallaxsports.external.basketball.dto.BalldontlieEnvelopeDto;
import dev.parallaxsports.external.basketball.dto.BalldontlieGameDto;
import dev.parallaxsports.external.basketball.dto.BalldontlieMetaDto;
import dev.parallaxsports.external.basketball.dto.BasketballSyncResponse;
import dev.parallaxsports.notification.event.EventsIngestedEvent;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class BasketballSyncService {

    private static final int DEFAULT_MAX_PAGES = 10;

    private final BalldontlieBasketballClient balldontlieBasketballClient;
    private final BasketballSyncWriteService basketballSyncWriteService;
    private final BasketballReadService basketballReadService;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional
    public BasketballSyncResponse syncSchedulerWindow(BasketballLeague league, LocalDate executionDate) {
        LocalDate from = resolveStartDate(league, executionDate);
        return syncGamesInternal(league, from, null, DEFAULT_MAX_PAGES);
    }

    @Transactional
    public BasketballSyncResponse syncRange(BasketballLeague league, LocalDate fromDate, LocalDate toDate, Integer maxPages) {
        if (fromDate == null) {
            throw new BadRequestException("fromDate is required");
        }
        int budget = maxPages != null && maxPages > 0 ? maxPages : DEFAULT_MAX_PAGES;
        return syncGamesInternal(league, fromDate, toDate, budget);
    }

    public List<BasketballGameResponse> getGames(BasketballLeague league, LocalDate fromDate, LocalDate toDate) {
        return basketballReadService.getGames(league, fromDate, toDate);
    }

    public List<BasketballTeamResponse> getTeams(BasketballLeague league) {
        return basketballReadService.getTeams(league);
    }

    private BasketballSyncResponse syncGamesInternal(BasketballLeague league, LocalDate fromDate, LocalDate toDate, int maxPages) {
        int pagesUsed = 0;
        int gamesFetched = 0;
        int gamesUpserted = 0;
        boolean stoppedByRateLimit = false;
        Long cursor = null;
        LocalDate maxProcessedDate = fromDate;
        Set<LocalDate> distinctDates = new HashSet<>();
        List<Long> ingestedEventIds = new ArrayList<>();

        while (pagesUsed < maxPages) {
            BalldontlieEnvelopeDto<BalldontlieGameDto> envelope;
            try {
                envelope = toDate != null
                    ? balldontlieBasketballClient.fetchGamesPage(league, fromDate, toDate, cursor)
                    : balldontlieBasketballClient.fetchGamesPage(league, fromDate, cursor);
            } catch (UpstreamServiceException ex) {
                if (isRateLimit(ex)) {
                    stoppedByRateLimit = true;
                    log.warn("Basketball sync rate-limited league={} after {} pages startDate={} cursor={}",
                        league, pagesUsed, fromDate, cursor);
                    break;
                }
                throw ex;
            }

            pagesUsed++;
            List<BalldontlieGameDto> games = envelope.data() == null ? List.of() : envelope.data();
            gamesFetched += games.size();

            for (BalldontlieGameDto game : games) {
                BasketballSyncWriteService.UpsertGameResult result = basketballSyncWriteService.upsertGame(league, game);
                if (result.changed()) {
                    gamesUpserted++;
                }
                if (result.eventId() != null) {
                    ingestedEventIds.add(result.eventId());
                }
                LocalDate gameDate = parseGameDate(game);
                if (gameDate != null) {
                    distinctDates.add(gameDate);
                    if (gameDate.isAfter(maxProcessedDate)) {
                        maxProcessedDate = gameDate;
                    }
                }
            }

            BalldontlieMetaDto meta = envelope.meta();
            cursor = meta == null ? null : meta.next_cursor();
            if (cursor == null) {
                break;
            }
        }

        boolean incomplete = (pagesUsed >= maxPages && cursor != null) || stoppedByRateLimit;

        if (!ingestedEventIds.isEmpty()) {
            eventPublisher.publishEvent(new EventsIngestedEvent(List.copyOf(ingestedEventIds)));
        }

        log.info(
            "Basketball sync finished league={} fromDate={} toDate={} pagesUsed={}/{} datesSynced={} gamesFetched={} gamesUpserted={} incomplete={} rateLimited={}",
            league, fromDate, toDate, pagesUsed, maxPages, distinctDates.size(), gamesFetched, gamesUpserted, incomplete, stoppedByRateLimit
        );

        return new BasketballSyncResponse(
            fromDate,
            maxProcessedDate,
            maxPages,
            pagesUsed,
            distinctDates.size(),
            gamesFetched,
            gamesUpserted,
            incomplete ? maxProcessedDate.plusDays(1) : null,
            incomplete
        );
    }

    private boolean isRateLimit(UpstreamServiceException ex) {
        Throwable current = ex;
        while (current != null) {
            if (current instanceof HttpClientErrorException.TooManyRequests) {
                return true;
            }
            if (current instanceof RestClientResponseException responseException
                && responseException.getStatusCode().value() == 429) {
                return true;
            }
            current = current.getCause();
        }
        return false;
    }

    private LocalDate resolveStartDate(BasketballLeague league, LocalDate executionDate) {
        LocalDate latest = basketballSyncWriteService.latestSyncedDate(league);
        return latest == null ? executionDate : latest;
    }

    private LocalDate parseGameDate(BalldontlieGameDto game) {
        if (game == null || game.date() == null || game.date().isBlank()) {
            return null;
        }
        try {
            return LocalDate.parse(game.date());
        } catch (RuntimeException ignored) {
            return null;
        }
    }
}
