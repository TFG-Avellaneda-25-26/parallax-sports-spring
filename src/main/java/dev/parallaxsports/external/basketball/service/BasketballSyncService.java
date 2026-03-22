package dev.parallaxsports.external.basketball.service;

import dev.parallaxsports.basketball.BasketballLeague;
import dev.parallaxsports.basketball.dto.BasketballGameResponse;
import dev.parallaxsports.basketball.dto.BasketballTeamResponse;
import dev.parallaxsports.basketball.service.BasketballTeamLogoResolver;
import dev.parallaxsports.core.exception.BadRequestException;
import dev.parallaxsports.core.exception.UpstreamServiceException;
import dev.parallaxsports.external.basketball.client.BalldontlieBasketballClient;
import dev.parallaxsports.external.basketball.dto.BalldontlieEnvelopeDto;
import dev.parallaxsports.external.basketball.dto.BalldontlieGameDto;
import dev.parallaxsports.external.basketball.dto.BalldontlieMetaDto;
import dev.parallaxsports.external.basketball.dto.BalldontlieTeamDto;
import dev.parallaxsports.external.basketball.dto.BasketballSyncResponse;
import dev.parallaxsports.formula1.model.Event;
import dev.parallaxsports.formula1.model.EventEntry;
import dev.parallaxsports.formula1.repository.EventEntryRepository;
import dev.parallaxsports.formula1.repository.EventRepository;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
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
    private final EventEntryRepository eventEntryRepository;

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

    @Transactional(readOnly = true)
    public List<BasketballGameResponse> getGames(BasketballLeague league, LocalDate fromDate, LocalDate toDate) {
        OffsetDateTime from = fromDate.atStartOfDay().atOffset(OffsetDateTime.now().getOffset());
        OffsetDateTime to = toDate.plusDays(1).atStartOfDay().atOffset(OffsetDateTime.now().getOffset()).minusNanos(1);

        List<String> providers = league != null
            ? List.of(league.getProvider())
            : Arrays.stream(BasketballLeague.values()).map(BasketballLeague::getProvider).toList();

        List<Event> events = eventRepository.findWithCompetitionByProvidersAndTimeBetween(providers, from, to);

        if (events.isEmpty()) {
            return List.of();
        }

        Set<Long> eventIds = events.stream().map(Event::getId).collect(Collectors.toSet());
        Map<Long, List<EventEntry>> entriesByEvent = eventEntryRepository.findWithParticipantByEventIdIn(eventIds)
            .stream()
            .collect(Collectors.groupingBy(ee -> ee.getId().getEventId()));

        return events.stream()
            .map(event -> toResponse(event, entriesByEvent.getOrDefault(event.getId(), List.of())))
            .toList();
    }

    public List<BasketballTeamResponse> getTeams(BasketballLeague league) {
        List<BasketballTeamResponse> all = new ArrayList<>();
        Long cursor = null;

        do {
            BalldontlieEnvelopeDto<BalldontlieTeamDto> envelope = balldontlieBasketballClient.fetchTeamsPage(league, cursor);
            List<BalldontlieTeamDto> teams = envelope.data() == null ? List.of() : envelope.data();
            for (BalldontlieTeamDto team : teams) {
                if (team.conference() == null || team.conference().isBlank()) {
                    continue;
                }
                all.add(new BasketballTeamResponse(
                    team.id(),
                    team.name(),
                    team.fullName(),
                    team.abbreviation(),
                    team.conference(),
                    team.division(),
                    team.city(),
                    BasketballTeamLogoResolver.resolveLogoUrl(league, team.abbreviation())
                ));
            }
            BalldontlieMetaDto meta = envelope.meta();
            cursor = meta == null ? null : meta.next_cursor();
        } while (cursor != null);

        return all;
    }

    private BasketballSyncResponse syncGamesInternal(BasketballLeague league, LocalDate fromDate, LocalDate toDate, int maxPages) {
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
                gamesUpserted += basketballSyncWriteService.upsertGame(league, game);
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

    private BasketballGameResponse toResponse(Event event, List<EventEntry> entries) {
        var competition = event.getCompetition();
        BasketballLeague league = BasketballLeague.fromCompetitionName(
            competition != null ? competition.getName() : null
        );

        String homeTeam = null;
        Long homeTeamId = null;
        String homeTeamLogo = null;
        String awayTeam = null;
        Long awayTeamId = null;
        String awayTeamLogo = null;

        for (EventEntry entry : entries) {
            if ("home".equals(entry.getSide())) {
                homeTeamId = entry.getParticipant().getId();
                homeTeam = entry.getParticipant().getName();
                homeTeamLogo = BasketballTeamLogoResolver.resolveLogoUrl(league, entry.getParticipant().getShortName());
            } else if ("away".equals(entry.getSide())) {
                awayTeamId = entry.getParticipant().getId();
                awayTeam = entry.getParticipant().getName();
                awayTeamLogo = BasketballTeamLogoResolver.resolveLogoUrl(league, entry.getParticipant().getShortName());
            }
        }

        return new BasketballGameResponse(
            event.getId(),
            event.getStartTimeUtc(),
            event.getStatus(),
            event.getEventType(),
            competition != null ? competition.getId() : null,
            competition != null ? competition.getName() : null,
            homeTeamId,
            homeTeam,
            homeTeamLogo,
            awayTeamId,
            awayTeam,
            awayTeamLogo
        );
    }
}
