package dev.parallaxsports.external.basketball.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import dev.parallaxsports.basketball.service.NbaTeamLogoResolver;
import dev.parallaxsports.external.basketball.dto.BalldontlieGameDto;
import dev.parallaxsports.formula1.model.Competition;
import dev.parallaxsports.formula1.model.Event;
import dev.parallaxsports.formula1.model.EventEntry;
import dev.parallaxsports.formula1.model.EventEntryId;
import dev.parallaxsports.formula1.model.MediaAsset;
import dev.parallaxsports.formula1.model.Participant;
import dev.parallaxsports.formula1.model.Season;
import dev.parallaxsports.formula1.model.Sport;
import dev.parallaxsports.formula1.repository.CompetitionRepository;
import dev.parallaxsports.formula1.repository.EventRepository;
import dev.parallaxsports.formula1.repository.EventEntryRepository;
import dev.parallaxsports.formula1.repository.MediaAssetRepository;
import dev.parallaxsports.formula1.repository.ParticipantRepository;
import dev.parallaxsports.formula1.repository.SeasonRepository;
import dev.parallaxsports.formula1.repository.SportRepository;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.Objects;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
class BasketballSyncWriteService {

    static final String PROVIDER = "balldontlie-basketball";
    private static final String SPORT_KEY = "basketball";
    private static final String SPORT_NAME = "Basketball";

    private final SportRepository sportRepository;
    private final CompetitionRepository competitionRepository;
    private final SeasonRepository seasonRepository;
    private final EventRepository eventRepository;
    private final ParticipantRepository participantRepository;
    private final EventEntryRepository eventEntryRepository;
    private final MediaAssetRepository mediaAssetRepository;
    private final ObjectMapper objectMapper;

    int upsertGame(BalldontlieGameDto game) {
        if (game.id() == null || game.datetime() == null || game.homeTeam() == null || game.visitorTeam() == null) {
            return 0;
        }

        Sport sport = ensureSport();
        Competition competition = ensureCompetition(sport, game);
        Season season = ensureSeason(competition, String.valueOf(game.season()));
        String externalId = "game:" + game.id();
        Event event = eventRepository.findByExternalProviderAndExternalId(PROVIDER, externalId).orElse(null);
        boolean created = false;

        if (event == null) {
            event = Event.builder()
                .sport(sport)
                .competition(competition)
                .season(season)
                .participantsMode("teams")
                .externalProvider(PROVIDER)
                .externalId(externalId)
                .build();
            created = true;
        }

        OffsetDateTime start = parseDate(game.datetime());
        boolean changed = false;
        changed |= setIfChanged(event.getName(), gameName(game), event::setName);
        changed |= setIfChanged(event.getEventType(), resolveEventType(game), event::setEventType);
        changed |= setIfChanged(event.getStatus(), normalizeStatus(game.status()), event::setStatus);
        changed |= setIfChanged(event.getStartTimeUtc(), start, event::setStartTimeUtc);
        changed |= setIfChanged(event.getEndTimeUtc(), null, event::setEndTimeUtc);
        changed |= setIfChanged(event.getMetadata(), buildMetadata(game), event::setMetadata);

        if (!sameEntityId(event.getCompetition(), competition)) {
            event.setCompetition(competition);
            changed = true;
        }
        if (!sameEntityId(event.getSeason(), season)) {
            event.setSeason(season);
            changed = true;
        }
        Event savedEvent = (!created && !changed) ? event : eventRepository.save(event);
        upsertEventParticipants(savedEvent, sport, game);
        return (!created && !changed) ? 0 : 1;
    }

    LocalDate latestSyncedDate() {
        return eventRepository.findFirstByExternalProviderOrderByStartTimeUtcDesc(PROVIDER)
            .map(Event::getStartTimeUtc)
            .map(OffsetDateTime::toLocalDate)
            .orElse(null);
    }

    private Sport ensureSport() {
        return sportRepository.findByKey(SPORT_KEY).orElseGet(() ->
            sportRepository.save(Sport.builder().key(SPORT_KEY).name(SPORT_NAME).build())
        );
    }

    private Competition ensureCompetition(Sport sport, BalldontlieGameDto game) {
        String competitionName = "NBA";
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

        ObjectNode metadata = objectMapper.createObjectNode();
        metadata.put("key", "nba");

        boolean changed = false;
        changed |= setIfChanged(competition.getCountry(), "United States", competition::setCountry);
        changed |= setIfChanged(competition.getRegion(), "US", competition::setRegion);
        changed |= setIfChanged(competition.getMetadata(), metadata, competition::setMetadata);

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

    private JsonNode buildMetadata(BalldontlieGameDto game) {
        ObjectNode root = objectMapper.createObjectNode();
        root.put("externalGameId", game.id());
        root.put("postseason", game.postseason());
        root.put("period", game.period());
        root.put("time", game.time());

        ObjectNode league = root.putObject("league");
        league.put("id", 1L);
        league.put("name", "NBA");
        league.put("season", String.valueOf(game.season()));

        ObjectNode teams = root.putObject("teams");
        if (game.homeTeam() != null) {
            ObjectNode home = teams.putObject("home");
            home.put("id", game.homeTeam().id());
            home.put("name", game.homeTeam().fullName());
            home.put("logo", NbaTeamLogoResolver.resolveLogoUrl(game.homeTeam().abbreviation()));
        }
        if (game.visitorTeam() != null) {
            ObjectNode away = teams.putObject("away");
            away.put("id", game.visitorTeam().id());
            away.put("name", game.visitorTeam().fullName());
            away.put("logo", NbaTeamLogoResolver.resolveLogoUrl(game.visitorTeam().abbreviation()));
        }

        ObjectNode scores = root.putObject("scores");
        scores.put("homeTotal", game.homeTeamScore());
        scores.put("awayTotal", game.visitorTeamScore());

        root.put("venue", (String) null);

        return root;
    }

    private void upsertEventParticipants(Event event, Sport sport, BalldontlieGameDto game) {
        if (event.getId() == null) {
            return;
        }

        Participant home = upsertTeamParticipant(sport, game.homeTeam(), "United States");
        Participant away = upsertTeamParticipant(sport, game.visitorTeam(), "United States");

        upsertEventEntry(event, home, "home", 1);
        upsertEventEntry(event, away, "away", 2);
    }

    private Participant upsertTeamParticipant(
        Sport sport,
        BalldontlieGameDto.TeamDto team,
        String country
    ) {
        if (team == null || team.id() == null) {
            return null;
        }

        Participant participant = participantRepository
            .findBySportIdAndExternalTeamId(sport.getId(), String.valueOf(team.id()))
            .orElse(null);
        boolean created = false;

        if (participant == null) {
            participant = participantRepository.findBySportIdAndKindAndName(sport.getId(), "team", nullSafe(team.name(), "Unknown Team"))
                .orElse(null);
        }

        if (participant == null) {
            participant = Participant.builder()
                .sport(sport)
                .kind("team")
                .build();
            created = true;
        }

        ObjectNode metadata = participant.getMetadata() instanceof ObjectNode existing
            ? existing.deepCopy()
            : objectMapper.createObjectNode();
        metadata.put("externalTeamId", team.id());
        metadata.put("abbreviation", team.abbreviation());
        metadata.put("logo", NbaTeamLogoResolver.resolveLogoUrl(team.abbreviation()));

        boolean changed = false;
        changed |= setIfChanged(participant.getName(), nullSafe(team.name(), "Unknown Team"), participant::setName);
        changed |= setIfChanged(participant.getShortName(), shortName(team.name()), participant::setShortName);
        changed |= setIfChanged(participant.getCountry(), country, participant::setCountry);
        changed |= setIfChanged(participant.getMetadata(), metadata, participant::setMetadata);

        Participant savedParticipant = (!created && !changed) ? participant : participantRepository.save(participant);
        upsertParticipantLogo(savedParticipant, team);
        return savedParticipant;
    }

    private void upsertParticipantLogo(Participant participant, BalldontlieGameDto.TeamDto team) {
        if (participant == null || participant.getId() == null || team == null) {
            return;
        }

        String logoUrl = NbaTeamLogoResolver.resolveLogoUrl(team.abbreviation());
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
            .sourceProvider(PROVIDER)
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

    private OffsetDateTime parseDate(String value) {
        return OffsetDateTime.parse(value);
    }

    private String nullSafe(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }

    private String shortName(String name) {
        if (name == null || name.isBlank()) {
            return null;
        }
        if (name.length() <= 24) {
            return name;
        }
        return name.substring(0, 24);
    }

    private boolean sameEntityId(Object left, Object right) {
        if (left == null && right == null) {
            return true;
        }
        if (left == null || right == null) {
            return false;
        }
        if (left instanceof Competition leftCompetition && right instanceof Competition rightCompetition) {
            return Objects.equals(leftCompetition.getId(), rightCompetition.getId());
        }
        if (left instanceof Season leftSeason && right instanceof Season rightSeason) {
            return Objects.equals(leftSeason.getId(), rightSeason.getId());
        }
        return false;
    }

    private <T> boolean setIfChanged(T current, T next, java.util.function.Consumer<T> setter) {
        if (Objects.equals(current, next)) {
            return false;
        }
        setter.accept(next);
        return true;
    }
}
