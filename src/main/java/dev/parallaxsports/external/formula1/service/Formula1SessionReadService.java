package dev.parallaxsports.external.formula1.service;

import dev.parallaxsports.core.exception.ResourceNotFoundException;
import dev.parallaxsports.formula1.dto.Formula1SessionResponse;
import dev.parallaxsports.formula1.model.Competition;
import dev.parallaxsports.formula1.model.Event;
import dev.parallaxsports.formula1.model.MediaAsset;
import dev.parallaxsports.formula1.model.Season;
import dev.parallaxsports.formula1.model.Sport;
import dev.parallaxsports.formula1.model.Venue;
import dev.parallaxsports.formula1.repository.CompetitionRepository;
import dev.parallaxsports.formula1.repository.EventRepository;
import dev.parallaxsports.formula1.repository.MediaAssetRepository;
import dev.parallaxsports.formula1.repository.SeasonRepository;
import dev.parallaxsports.formula1.repository.SportRepository;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
class Formula1SessionReadService {

    private static final String OPENF1_PROVIDER = "openf1";
    private static final String FORMULA1_KEY = "formula1";
    private static final String F1_COMPETITION_NAME = "Formula 1 World Championship";

    private final SportRepository sportRepository;
    private final CompetitionRepository competitionRepository;
    private final SeasonRepository seasonRepository;
    private final EventRepository eventRepository;
    private final MediaAssetRepository mediaAssetRepository;

    List<Formula1SessionResponse> getSessionsForYear(int year) {
        Sport sport = sportRepository.findByKey(FORMULA1_KEY)
            .orElseThrow(() -> new ResourceNotFoundException("Formula1 sport not found. Run sync first."));

        Competition competition = competitionRepository.findBySportIdAndName(sport.getId(), F1_COMPETITION_NAME)
            .orElseThrow(() -> new ResourceNotFoundException("Formula1 competition not found. Run sync first."));

        Season season = seasonRepository.findByCompetitionIdAndName(competition.getId(), String.valueOf(year))
            .orElseThrow(() -> new ResourceNotFoundException("Formula1 season not found for year " + year));

        List<Event> sessionEvents = eventRepository
            .findByExternalProviderAndExternalIdStartingWithAndSeasonIdOrderByStartTimeUtcAsc(
                OPENF1_PROVIDER,
                "session:",
                season.getId()
            );

        Map<Long, String> latestVenueLogos = mapLatestVenueLogos(sessionEvents);
        return sessionEvents.stream().map(event -> toSessionResponse(event, latestVenueLogos)).toList();
    }

    private Map<Long, String> mapLatestVenueLogos(List<Event> sessionEvents) {
        List<Long> venueIds = sessionEvents
            .stream()
            .map(Event::getVenue)
            .filter(Objects::nonNull)
            .map(Venue::getId)
            .filter(Objects::nonNull)
            .distinct()
            .toList();

        if (venueIds.isEmpty()) {
            return Map.of();
        }

        List<MediaAsset> logos = mediaAssetRepository.findByOwnerTypeAndOwnerIdInAndAssetTypeOrderByOwnerIdAscIdDesc(
            "venue",
            venueIds,
            "logo"
        );

        Map<Long, String> latestByVenue = new HashMap<>();
        for (MediaAsset logo : logos) {
            latestByVenue.putIfAbsent(logo.getOwnerId(), logo.getUrl());
        }
        return latestByVenue;
    }

    private Formula1SessionResponse toSessionResponse(Event event, Map<Long, String> latestVenueLogos) {
        Venue venue = event.getVenue();
        String logo = venue == null || venue.getId() == null ? null : latestVenueLogos.get(venue.getId());

        return new Formula1SessionResponse(
            event.getId(),
            venue == null ? null : venue.getName(),
            logo,
            event.getStartTimeUtc(),
            event.getEndTimeUtc(),
            event.getEventType(),
            event.getName(),
            event.getStage()
        );
    }
}
