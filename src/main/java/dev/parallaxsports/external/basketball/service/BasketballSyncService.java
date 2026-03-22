package dev.parallaxsports.external.basketball.service;

import com.fasterxml.jackson.databind.JsonNode;
import dev.parallaxsports.basketball.dto.BasketballGameResponse;
import dev.parallaxsports.basketball.dto.BasketballTeamResponse;
import dev.parallaxsports.basketball.service.NbaTeamLogoResolver;
import dev.parallaxsports.core.exception.BadRequestException;
import dev.parallaxsports.core.exception.UpstreamServiceException;
import dev.parallaxsports.external.basketball.client.BalldontlieBasketballClient;
import dev.parallaxsports.external.basketball.dto.BalldontlieEnvelopeDto;
import dev.parallaxsports.external.basketball.dto.BalldontlieGameDto;
import dev.parallaxsports.external.basketball.dto.BalldontlieMetaDto;
import dev.parallaxsports.external.basketball.dto.BalldontlieTeamDto;
import dev.parallaxsports.external.basketball.dto.BasketballSyncResponse;
import dev.parallaxsports.formula1.model.Event;
import dev.parallaxsports.formula1.repository.EventRepository;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
    private final EventRepository eventRepository;

    /**
     * Daily scheduler entry point: syncs from the latest previously synced date forward.
     */
    @Transactional
    public BasketballSyncResponse syncSchedulerWindow(LocalDate executionDate) {
        LocalDate from = resolveStartDate(executionDate);
        return syncGamesInternal(from, null, DEFAULT_MAX_PAGES);
    }

    /**
     * Admin entry point: syncs games within a date range with a configurable page budget.
     */
    @Transactional
    public BasketballSyncResponse syncRange(LocalDate fromDate, LocalDate toDate, Integer maxPages) {
        if (fromDate == null) {
            throw new BadRequestException("fromDate is required");
        }
        int budget = maxPages != null && maxPages > 0 ? maxPages : DEFAULT_MAX_PAGES;
        return syncGamesInternal(fromDate, toDate, budget);
    }

    @Transactional(readOnly = true)
    public List<BasketballGameResponse> getGames(LocalDate fromDate, LocalDate toDate) {
        OffsetDateTime from = fromDate.atStartOfDay().atOffset(OffsetDateTime.now().getOffset());
        OffsetDateTime to = toDate.plusDays(1).atStartOfDay().atOffset(OffsetDateTime.now().getOffset()).minusNanos(1);

        return eventRepository.findByExternalProviderAndStartTimeUtcBetweenOrderByStartTimeUtcAsc(
                BasketballSyncWriteService.PROVIDER,
                from,
                to
            )
            .stream()
            .map(this::toResponse)
            .toList();
    }

    public List<BasketballTeamResponse> getTeams() {
        List<BasketballTeamResponse> all = new ArrayList<>();
        Long cursor = null;

        do {
            BalldontlieEnvelopeDto<BalldontlieTeamDto> envelope = balldontlieBasketballClient.fetchTeamsPage(cursor);
            List<BalldontlieTeamDto> teams = envelope.data() == null ? List.of() : envelope.data();
            for (BalldontlieTeamDto team : teams) {
                all.add(new BasketballTeamResponse(
                    team.id(),
                    team.name(),
                    team.fullName(),
                    team.abbreviation(),
                    team.conference(),
                    team.division(),
                    team.city(),
                    NbaTeamLogoResolver.resolveLogoUrl(team.abbreviation())
                ));
            }
            BalldontlieMetaDto meta = envelope.meta();
            cursor = meta == null ? null : meta.next_cursor();
        } while (cursor != null);

        return all;
    }

    private BasketballSyncResponse syncGamesInternal(LocalDate fromDate, LocalDate toDate, int maxPages) {
        int pagesUsed = 0;
        int gamesFetched = 0;
        int gamesUpserted = 0;
        boolean stoppedByRateLimit = false;
        Long cursor = null;
        LocalDate maxProcessedDate = fromDate;
        Set<LocalDate> distinctDates = new HashSet<>();

        while (pagesUsed < maxPages) {
            BalldontlieEnvelopeDto<BalldontlieGameDto> envelope;
            try {
                envelope = toDate != null
                    ? balldontlieBasketballClient.fetchGamesPage(fromDate, toDate, cursor)
                    : balldontlieBasketballClient.fetchGamesPage(fromDate, cursor);
            } catch (UpstreamServiceException ex) {
                if (isRateLimit(ex)) {
                    stoppedByRateLimit = true;
                    log.warn("Basketball sync rate-limited after {} pages startDate={} cursor={}",
                        pagesUsed, fromDate, cursor);
                    break;
                }
                throw ex;
            }

            pagesUsed++;
            List<BalldontlieGameDto> games = envelope.data() == null ? List.of() : envelope.data();
            gamesFetched += games.size();

            for (BalldontlieGameDto game : games) {
                gamesUpserted += basketballSyncWriteService.upsertGame(game);
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

        log.info(
            "Basketball sync finished fromDate={} toDate={} pagesUsed={}/{} datesSynced={} gamesFetched={} gamesUpserted={} incomplete={} rateLimited={}",
            fromDate, toDate, pagesUsed, maxPages, distinctDates.size(), gamesFetched, gamesUpserted, incomplete, stoppedByRateLimit
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

    private LocalDate resolveStartDate(LocalDate executionDate) {
        LocalDate latest = basketballSyncWriteService.latestSyncedDate();
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

    private BasketballGameResponse toResponse(Event event) {
        JsonNode metadata = event.getMetadata();
        JsonNode league = metadata == null ? null : metadata.path("league");
        JsonNode teams = metadata == null ? null : metadata.path("teams");
        JsonNode scores = metadata == null ? null : metadata.path("scores");

        return new BasketballGameResponse(
            event.getId(),
            metadata == null ? null : metadata.path("externalGameId").asLong(),
            event.getStartTimeUtc(),
            event.getStatus(),
            league == null ? null : asLongOrNull(league.path("id")),
            league == null ? null : asTextOrNull(league.path("name")),
            league == null ? null : asTextOrNull(league.path("season")),
            teams == null ? null : asLongOrNull(teams.path("home").path("id")),
            teams == null ? null : asTextOrNull(teams.path("home").path("name")),
            teams == null ? null : asTextOrNull(teams.path("home").path("logo")),
            scores == null ? null : asIntOrNull(scores.path("homeTotal")),
            teams == null ? null : asLongOrNull(teams.path("away").path("id")),
            teams == null ? null : asTextOrNull(teams.path("away").path("name")),
            teams == null ? null : asTextOrNull(teams.path("away").path("logo")),
            scores == null ? null : asIntOrNull(scores.path("awayTotal")),
            metadata == null ? null : asTextOrNull(metadata.path("venue"))
        );
    }

    private Long asLongOrNull(JsonNode node) {
        return node == null || node.isMissingNode() || node.isNull() ? null : node.asLong();
    }

    private Integer asIntOrNull(JsonNode node) {
        return node == null || node.isMissingNode() || node.isNull() ? null : node.asInt();
    }

    private String asTextOrNull(JsonNode node) {
        return node == null || node.isMissingNode() || node.isNull() ? null : node.asText();
    }
}
