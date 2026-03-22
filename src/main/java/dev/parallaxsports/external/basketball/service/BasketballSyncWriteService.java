package dev.parallaxsports.external.basketball.service;

import static dev.parallaxsports.external.sync.SyncWriteHelper.nullSafe;
import static dev.parallaxsports.external.sync.SyncWriteHelper.sameEntityId;
import static dev.parallaxsports.external.sync.SyncWriteHelper.setIfChanged;

import dev.parallaxsports.sport.basketball.BasketballLeague;
import dev.parallaxsports.sport.basketball.service.BasketballTeamLogoResolver;
import dev.parallaxsports.external.basketball.dto.BalldontlieGameDto;
import dev.parallaxsports.sport.model.Competition;
import dev.parallaxsports.sport.model.Event;
import dev.parallaxsports.sport.model.EventEntry;
import dev.parallaxsports.sport.model.EventEntryId;
import dev.parallaxsports.sport.model.MediaAsset;
import dev.parallaxsports.sport.model.Participant;
import dev.parallaxsports.sport.model.Season;
import dev.parallaxsports.sport.model.Sport;
import dev.parallaxsports.sport.repository.CompetitionRepository;
import dev.parallaxsports.sport.repository.EventRepository;
import dev.parallaxsports.sport.repository.EventEntryRepository;
import dev.parallaxsports.sport.repository.MediaAssetRepository;
import dev.parallaxsports.sport.repository.ParticipantRepository;
import dev.parallaxsports.sport.repository.SeasonRepository;
import dev.parallaxsports.sport.repository.SportRepository;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
class BasketballSyncWriteService {

    private static final String SPORT_KEY = "basketball";
    private static final String SPORT_NAME = "Basketball";

    private final SportRepository sportRepository;
    private final CompetitionRepository competitionRepository;
    private final SeasonRepository seasonRepository;
    private final EventRepository eventRepository;
    private final ParticipantRepository participantRepository;
    private final EventEntryRepository eventEntryRepository;
    private final MediaAssetRepository mediaAssetRepository;

    int upsertGame(BasketballLeague league, BalldontlieGameDto game) {
        String rawDateTime = resolveDateTime(game);
        if (game.id() == null || rawDateTime == null || game.homeTeam() == null || game.visitorTeam() == null) {
            return 0;
        }

        String provider = league.getProvider();
        Sport sport = ensureSport();
        Competition competition = ensureCompetition(sport, league);
        Season season = ensureSeason(competition, String.valueOf(game.season()));
        String externalId = "game:" + game.id();
        Event event = eventRepository.findByExternalProviderAndExternalId(provider, externalId).orElse(null);
        boolean created = false;

        if (event == null) {
            event = Event.builder()
                .sport(sport)
                .competition(competition)
                .season(season)
                .participantsMode("teams")
                .externalProvider(provider)
                .externalId(externalId)
                .build();
            created = true;
        }

        OffsetDateTime start = parseDate(rawDateTime);
        boolean changed = false;
        changed |= setIfChanged(event.getName(), gameName(game), event::setName);
        changed |= setIfChanged(event.getEventType(), resolveEventType(game), event::setEventType);
        changed |= setIfChanged(event.getStatus(), normalizeStatus(game.status()), event::setStatus);
        changed |= setIfChanged(event.getStartTimeUtc(), start, event::setStartTimeUtc);
        changed |= setIfChanged(event.getEndTimeUtc(), null, event::setEndTimeUtc);

        if (!sameEntityId(event.getCompetition(), competition)) {
            event.setCompetition(competition);
            changed = true;
        }
        if (!sameEntityId(event.getSeason(), season)) {
            event.setSeason(season);
            changed = true;
        }
        Event savedEvent = (!created && !changed) ? event : eventRepository.save(event);
        upsertEventParticipants(league, savedEvent, sport, game);
        return (!created && !changed) ? 0 : 1;
    }

    LocalDate latestSyncedDate(BasketballLeague league) {
        return eventRepository.findFirstByExternalProviderOrderByStartTimeUtcDesc(league.getProvider())
            .map(Event::getStartTimeUtc)
            .map(OffsetDateTime::toLocalDate)
            .orElse(null);
    }

    private Sport ensureSport() {
        return sportRepository.findByKey(SPORT_KEY).orElseGet(() ->
            sportRepository.save(Sport.builder().key(SPORT_KEY).name(SPORT_NAME).build())
        );
    }

    private Competition ensureCompetition(Sport sport, BasketballLeague league) {
        String competitionName = league.getCompetitionName();
        Competition competition = competitionRepository.findBySportIdAndName(sport.getId(), competitionName).orElse(null);
        boolean created = false;

        if (competition == null) {
            competition = Competition.builder()
                .sport(sport)
                .name(competitionName)
                .kind("league")
                .build();
            created = true;
        }

        boolean changed = false;
        changed |= setIfChanged(competition.getCountry(), "United States", competition::setCountry);
        changed |= setIfChanged(competition.getRegion(), "US", competition::setRegion);

        if (!created && !changed) {
            return competition;
        }

        return competitionRepository.save(competition);
    }

    private Season ensureSeason(Competition competition, String seasonName) {
        String resolved = nullSafe(seasonName, String.valueOf(LocalDate.now().getYear()));
        Season season = seasonRepository.findByCompetitionIdAndName(competition.getId(), resolved).orElse(null);
        if (season != null) {
            return season;
        }

        return seasonRepository.save(
            Season.builder()
                .competition(competition)
                .name(resolved)
                .build()
        );
    }

    private void upsertEventParticipants(BasketballLeague league, Event event, Sport sport, BalldontlieGameDto game) {
        if (event.getId() == null) {
            return;
        }

        Participant home = upsertTeamParticipant(league, sport, game.homeTeam(), "United States");
        Participant away = upsertTeamParticipant(league, sport, game.visitorTeam(), "United States");

        upsertEventEntry(event, home, "home", 1);
        upsertEventEntry(event, away, "away", 2);
    }

    private Participant upsertTeamParticipant(
        BasketballLeague league,
        Sport sport,
        BalldontlieGameDto.TeamDto team,
        String country
    ) {
        if (team == null || team.id() == null) {
            return null;
        }

        Participant participant = participantRepository
            .findBySportIdAndKindAndName(sport.getId(), "team", nullSafe(team.name(), "Unknown Team"))
            .orElse(null);
        boolean created = false;

        if (participant == null) {
            participant = Participant.builder()
                .sport(sport)
                .kind("team")
                .build();
            created = true;
        }

        boolean changed = false;
        changed |= setIfChanged(participant.getName(), nullSafe(team.name(), "Unknown Team"), participant::setName);
        changed |= setIfChanged(participant.getShortName(), team.abbreviation(), participant::setShortName);
        changed |= setIfChanged(participant.getCountry(), country, participant::setCountry);

        Participant savedParticipant = (!created && !changed) ? participant : participantRepository.save(participant);
        upsertParticipantLogo(league, savedParticipant, team);
        return savedParticipant;
    }

    private void upsertParticipantLogo(BasketballLeague league, Participant participant, BalldontlieGameDto.TeamDto team) {
        if (participant == null || participant.getId() == null || team == null) {
            return;
        }

        String logoUrl = BasketballTeamLogoResolver.resolveLogoUrl(league, team.abbreviation());
        if (logoUrl == null || logoUrl.isBlank()) {
            return;
        }

        boolean exists = mediaAssetRepository.existsByOwnerTypeAndOwnerIdAndAssetTypeAndUrl(
            "participant",
            participant.getId(),
            "logo",
            logoUrl
        );
        if (exists) {
            return;
        }

        MediaAsset asset = MediaAsset.builder()
            .ownerType("participant")
            .ownerId(participant.getId())
            .assetType("logo")
            .url(logoUrl)
            .contentType("image/png")
            .altText(participant.getName() + " team logo")
            .sourceProvider(league.getProvider())
            .sourceUrl(logoUrl)
            .build();

        mediaAssetRepository.save(asset);
    }

    private void upsertEventEntry(Event event, Participant participant, String side, int displayOrder) {
        if (event.getId() == null || participant == null || participant.getId() == null) {
            return;
        }

        EventEntryId id = new EventEntryId(event.getId(), participant.getId());
        EventEntry entry = eventEntryRepository.findById(id).orElse(null);
        boolean created = false;
        if (entry == null) {
            entry = EventEntry.builder()
                .id(id)
                .event(event)
                .participant(participant)
                .build();
            created = true;
        }

        boolean changed = false;
        changed |= setIfChanged(entry.getSide(), side, entry::setSide);
        changed |= setIfChanged(entry.getDisplayOrder(), displayOrder, entry::setDisplayOrder);

        if (!created && !changed) {
            return;
        }

        eventEntryRepository.save(entry);
    }

    private String gameName(BalldontlieGameDto game) {
        String home = game.homeTeam() == null ? "Home" : nullSafe(game.homeTeam().fullName(), "Home");
        String away = game.visitorTeam() == null ? "Away" : nullSafe(game.visitorTeam().fullName(), "Away");
        return home + " vs " + away;
    }

    private String normalizeStatus(String status) {
        if (status == null || status.isBlank()) {
            return "scheduled";
        }

        String normalized = status.toLowerCase();
        if (normalized.contains("final")) {
            return "finished";
        }
        if (normalized.contains("qtr") || normalized.contains("halftime") || normalized.contains("ot")) {
            return "live";
        }
        if (normalized.contains("postponed")) {
            return "postponed";
        }
        if (normalized.contains("canceled") || normalized.contains("cancelled")) {
            return "cancelled";
        }
        return "scheduled";
    }

    private String resolveEventType(BalldontlieGameDto game) {
        return Boolean.TRUE.equals(game.postseason()) ? "playoffs" : "game";
    }

    private String resolveDateTime(BalldontlieGameDto game) {
        if (game.datetime() != null && !game.datetime().isBlank()) {
            return game.datetime();
        }
        if (game.date() != null && !game.date().isBlank()) {
            return game.date();
        }
        return null;
    }

    private OffsetDateTime parseDate(String value) {
        try {
            return OffsetDateTime.parse(value);
        } catch (java.time.DateTimeException ex) {
            return java.time.Instant.parse(value).atOffset(java.time.ZoneOffset.UTC);
        }
    }

}
