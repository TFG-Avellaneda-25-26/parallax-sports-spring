package dev.parallaxsports.external.basketball.service;

import dev.parallaxsports.sport.basketball.BasketballLeague;
import dev.parallaxsports.sport.basketball.dto.BasketballGameResponse;
import dev.parallaxsports.sport.basketball.dto.BasketballTeamResponse;
import dev.parallaxsports.sport.basketball.service.BasketballTeamLogoResolver;
import dev.parallaxsports.external.basketball.client.BalldontlieBasketballClient;
import dev.parallaxsports.external.basketball.dto.BalldontlieEnvelopeDto;
import dev.parallaxsports.external.basketball.dto.BalldontlieMetaDto;
import dev.parallaxsports.external.basketball.dto.BalldontlieTeamDto;
import dev.parallaxsports.sport.model.Event;
import dev.parallaxsports.sport.model.EventEntry;
import dev.parallaxsports.sport.repository.EventEntryRepository;
import dev.parallaxsports.sport.repository.EventRepository;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
class BasketballReadService {

    private final BalldontlieBasketballClient balldontlieBasketballClient;
    private final EventRepository eventRepository;
    private final EventEntryRepository eventEntryRepository;

    @Transactional(readOnly = true)
    List<BasketballGameResponse> getGames(BasketballLeague league, LocalDate fromDate, LocalDate toDate) {
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

    List<BasketballTeamResponse> getTeams(BasketballLeague league) {
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
