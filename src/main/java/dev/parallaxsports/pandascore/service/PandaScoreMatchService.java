package dev.parallaxsports.pandascore.service;

import dev.parallaxsports.pandascore.dto.PandaScoreMatchResponse;
import dev.parallaxsports.sport.model.Event;
import dev.parallaxsports.sport.repository.EventRepository;
import java.time.OffsetDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class PandaScoreMatchService {

    private static final String EXTERNAL_PROVIDER = "pandascore";

    private final EventRepository eventRepository;

    @Transactional(readOnly = true)
    public List<PandaScoreMatchResponse> getAllMatches() {
        return eventRepository.findByExternalProviderOrderByStartTimeUtcDesc(EXTERNAL_PROVIDER)
            .stream()
            .map(this::mapToResponse)
            .toList();
    }

    @Transactional(readOnly = true)
    public List<PandaScoreMatchResponse> getMatchesByLeague(String leagueName) {
        return eventRepository.findByExternalProviderAndCompetitionNameOrderByStartTimeUtcDesc(EXTERNAL_PROVIDER, leagueName)
            .stream()
            .map(this::mapToResponse)
            .toList();
    }

    @Transactional(readOnly = true)
    public List<PandaScoreMatchResponse> getMatchesByVideogame(String videogame) {
        return eventRepository.findByExternalProviderAndSportKeyOrderByStartTimeUtcDesc(EXTERNAL_PROVIDER, videogame)
            .stream()
            .map(this::mapToResponse)
            .toList();
    }

    @Transactional(readOnly = true)
    public List<PandaScoreMatchResponse> getMatchesByVideogameBetweenDates(
        String videogame,
        OffsetDateTime startDate,
        OffsetDateTime endDate
    ) {
        return eventRepository.findByExternalProviderAndSportKeyAndStartTimeUtcBetweenOrderByStartTimeUtcDesc(
                EXTERNAL_PROVIDER,
                videogame,
                startDate,
                endDate
            )
            .stream()
            .map(this::mapToResponse)
            .toList();
    }

    @Transactional(readOnly = true)
    public List<PandaScoreMatchResponse> getMatchesByLeagueBetweenDates(
        String leagueName,
        OffsetDateTime startDate,
        OffsetDateTime endDate
    ) {
        return eventRepository.findByExternalProviderAndCompetitionNameAndStartTimeUtcBetweenOrderByStartTimeUtcDesc(
                EXTERNAL_PROVIDER,
                leagueName,
                startDate,
                endDate
            )
            .stream()
            .map(this::mapToResponse)
            .toList();
    }

    @Transactional(readOnly = true)
    public List<PandaScoreMatchResponse> getMatchesBetweenDates(OffsetDateTime startDate, OffsetDateTime endDate) {
        return eventRepository.findByExternalProviderAndStartTimeUtcBetweenOrderByStartTimeUtcDesc(EXTERNAL_PROVIDER, startDate, endDate)
            .stream()
            .map(this::mapToResponse)
            .toList();
    }

    private PandaScoreMatchResponse mapToResponse(Event event) {
        return new PandaScoreMatchResponse(
            event.getId(),
            event.getName(),
            event.getCompetition() == null ? null : event.getCompetition().getName(),
            event.getStatus(),
            event.getStage() != null ? event.getStage() : event.getExternalId(),
            event.getStartTimeUtc(),
            event.getEndTimeUtc(),
            event.getSport() == null ? null : event.getSport().getKey()
        );
    }
}

