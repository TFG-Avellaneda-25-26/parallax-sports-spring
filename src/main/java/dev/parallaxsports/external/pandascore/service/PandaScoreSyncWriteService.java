package dev.parallaxsports.external.pandascore.service;

import static dev.parallaxsports.external.sync.SyncWriteHelper.nullSafe;
import static dev.parallaxsports.external.sync.SyncWriteHelper.setIfChanged;

import dev.parallaxsports.external.pandascore.dto.PandaScoreMatchDto;
import dev.parallaxsports.external.pandascore.dto.PandaScoreOpponentDto;
import dev.parallaxsports.external.pandascore.dto.PandaScoreTeamDto;
import dev.parallaxsports.sport.model.Competition;
import dev.parallaxsports.sport.model.Event;
import dev.parallaxsports.sport.model.Sport;
import dev.parallaxsports.sport.repository.CompetitionRepository;
import dev.parallaxsports.sport.repository.EventRepository;
import dev.parallaxsports.sport.repository.SportRepository;
import java.time.OffsetDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class PandaScoreSyncWriteService {

    private static final String EXTERNAL_PROVIDER = "pandascore";

    private final SportRepository sportRepository;
    private final CompetitionRepository competitionRepository;
    private final EventRepository eventRepository;

    @Transactional
    public SyncCounters syncMatches(List<PandaScoreMatchDto> matches, String videogame) {
        int upserted = 0;

        for (PandaScoreMatchDto match : matches) {
            if (match == null || match.id() == null) {
                continue;
            }

            if (upsertMatch(match, videogame)) {
                upserted++;
            }
        }

        log.info("PandaScore sync completed: matches={}", upserted);
        return new SyncCounters(upserted);
    }

    private boolean upsertMatch(PandaScoreMatchDto dto, String videogame) {
        Sport sport = ensureSport(videogame);
        Competition competition = ensureCompetition(sport, dto, videogame);
        String externalId = externalId(dto);
        Event event = eventRepository.findByExternalProviderAndExternalId(EXTERNAL_PROVIDER, externalId).orElse(null);
        boolean created = false;

        if (event == null) {
            event = Event.builder()
                .sport(sport)
                .competition(competition)
                .participantsMode("teams")
                .externalProvider(EXTERNAL_PROVIDER)
                .externalId(externalId)
                .build();
            created = true;
        }

        boolean changed = false;
        changed |= setIfChanged(event.getSport(), sport, event::setSport);
        changed |= setIfChanged(event.getCompetition(), competition, event::setCompetition);
        changed |= setIfChanged(event.getEventType(), "match", event::setEventType);
        changed |= setIfChanged(event.getName(), resolveName(dto, competition), event::setName);
        changed |= setIfChanged(event.getStage(), resolveStage(dto), event::setStage);
        changed |= setIfChanged(event.getStatus(), normalizeStatus(dto.status()), event::setStatus);
        changed |= setIfChanged(event.getStartTimeUtc(), resolveStartTime(dto.beginAt()), event::setStartTimeUtc);
        changed |= setIfChanged(event.getEndTimeUtc(), resolveEndTime(dto.endAt()), event::setEndTimeUtc);
        changed |= setIfChanged(event.getParticipantsMode(), "teams", event::setParticipantsMode);
        changed |= setIfChanged(event.getExternalProvider(), EXTERNAL_PROVIDER, event::setExternalProvider);
        changed |= setIfChanged(event.getExternalId(), externalId, event::setExternalId);

        if (!created && !changed) {
            return false;
        }

        eventRepository.save(event);
        return true;
    }

    private Sport ensureSport(String videogame) {
        String key = nullSafe(videogame, "pandascore");
        return sportRepository.findByKey(key).orElseGet(() ->
            sportRepository.save(
                Sport.builder()
                    .key(key)
                    .name(resolveSportName(key))
                    .build()
            )
        );
    }

    private Competition ensureCompetition(Sport sport, PandaScoreMatchDto dto, String videogame) {
        String competitionName = resolveCompetitionName(dto, videogame);
        Competition competition = competitionRepository.findBySportIdAndName(sport.getId(), competitionName).orElse(null);
        if (competition != null) {
            boolean changed = false;
            changed |= setIfChanged(competition.getKind(), "league", competition::setKind);
            changed |= setIfChanged(competition.getRegion(), null, competition::setRegion);
            changed |= setIfChanged(competition.getCountry(), null, competition::setCountry);
            if (!changed) {
                return competition;
            }
        } else {
            competition = Competition.builder()
                .sport(sport)
                .name(competitionName)
                .kind("league")
                .build();
        }

        return competitionRepository.save(competition);
    }

    private String resolveName(PandaScoreMatchDto dto, Competition competition) {
        if (dto.name() != null && !dto.name().isBlank()) {
            return dto.name();
        }

        String opponentName = firstOpponentName(dto.opponents());
        if (opponentName != null) {
            return opponentName;
        }

        return (competition == null ? "PandaScore match" : competition.getName() + " match") + " " + dto.id();
    }

    private String resolveCompetitionName(PandaScoreMatchDto dto, String videogame) {
        if (dto.league() != null) {
            if (dto.league().name() != null && !dto.league().name().isBlank()) {
                return dto.league().name();
            }
            if (dto.league().slug() != null && !dto.league().slug().isBlank()) {
                return dto.league().slug();
            }
        }
        return resolveSportName(nullSafe(videogame, "pandascore"));
    }

    private String resolveSportName(String key) {
        return switch (key) {
            case "league-of-legends" -> "League of Legends";
            case "valorant" -> "Valorant";
            case "dota2" -> "Dota 2";
            case "counter-strike" -> "Counter-Strike";
            default -> "PandaScore";
        };
    }

    private String resolveStage(PandaScoreMatchDto dto) {
        if (dto.slug() != null && !dto.slug().isBlank()) {
            return dto.slug();
        }
        return null;
    }

    private String externalId(PandaScoreMatchDto dto) {
        return "match:" + dto.id();
    }

    private OffsetDateTime resolveStartTime(String beginAt) {
        OffsetDateTime parsed = parseDateTime(beginAt);
        return parsed != null ? parsed : OffsetDateTime.now();
    }

    private OffsetDateTime resolveEndTime(String endAt) {
        return parseDateTime(endAt);
    }

    private String firstOpponentName(PandaScoreOpponentDto[] opponents) {
        if (opponents == null) {
            return null;
        }

        for (PandaScoreOpponentDto opponent : opponents) {
            if (opponent == null) {
                continue;
            }
            PandaScoreTeamDto team = opponent.opponent();
            if (team != null && team.name() != null && !team.name().isBlank()) {
                return team.name();
            }
        }
        return null;
    }

    private OffsetDateTime parseDateTime(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }

        try {
            return OffsetDateTime.parse(value);
        } catch (Exception ex) {
            log.debug("Unable to parse PandaScore date '{}': {}", value, ex.getMessage());
            return null;
        }
    }

    private String normalizeStatus(String status) {
        if (status == null || status.isBlank()) {
            return "scheduled";
        }

        String normalized = status.toLowerCase();
        if (normalized.contains("not_started") || normalized.contains("scheduled") || normalized.contains("pending")) {
            return "scheduled";
        }
        if (normalized.contains("live") || normalized.contains("running") || normalized.contains("ongoing") || normalized.contains("in_progress")) {
            return "live";
        }
        if (normalized.contains("finished") || normalized.contains("complete") || normalized.contains("ended")) {
            return "finished";
        }
        if (normalized.contains("cancel")) {
            return "cancelled";
        }
        if (normalized.contains("postpon") || normalized.contains("delayed")) {
            return "postponed";
        }
        return "scheduled";
    }

    public record SyncCounters(int matchesUpserted) {
    }
}


