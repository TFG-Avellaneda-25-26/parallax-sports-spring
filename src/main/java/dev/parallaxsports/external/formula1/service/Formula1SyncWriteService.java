package dev.parallaxsports.external.formula1.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import dev.parallaxsports.external.formula1.dto.OpenF1MeetingDto;
import dev.parallaxsports.external.formula1.dto.OpenF1SessionDto;
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
import dev.parallaxsports.formula1.repository.VenueRepository;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
class Formula1SyncWriteService {

    private static final String OPENF1_PROVIDER = "openf1";
    private static final String FORMULA1_KEY = "formula1";
    private static final String FORMULA1_NAME = "Formula 1";
    private static final String F1_COMPETITION_NAME = "Formula 1 World Championship";
    private static final Pattern QUALIFYING_STAGE_PATTERN = Pattern.compile("\\b(Q[1-3])\\b", Pattern.CASE_INSENSITIVE);

    private final SportRepository sportRepository;
    private final CompetitionRepository competitionRepository;
    private final SeasonRepository seasonRepository;
    private final VenueRepository venueRepository;
    private final EventRepository eventRepository;
    private final MediaAssetRepository mediaAssetRepository;
    private final ObjectMapper objectMapper;

    SyncCounters syncSeason(int year, List<OpenF1MeetingDto> meetings, List<OpenF1SessionDto> sessions) {
        Sport sport = ensureSport();
        Competition competition = ensureCompetition(sport);
        Season season = ensureSeason(competition, year, meetings);

        Map<Long, Event> meetingEvents = new HashMap<>();

        int venuesUpserted = 0;
        int meetingsUpserted = 0;
        int sessionsUpserted = 0;

        for (OpenF1MeetingDto meeting : meetings) {
            if (meeting.meetingKey() == null || meeting.dateStart() == null) {
                continue;
            }

            UpsertVenueResult venueResult = upsertVenue(sport, meeting);
            if (venueResult.created()) {
                venuesUpserted++;
            }

            UpsertEventResult meetingEventResult = upsertMeetingEvent(sport, competition, season, venueResult.venue(), meeting);
            if (meetingEventResult.created()) {
                meetingsUpserted++;
            }
            meetingEvents.put(meeting.meetingKey(), meetingEventResult.event());

            upsertVenueLogo(venueResult.venue(), meeting);
        }

        for (OpenF1SessionDto session : sessions) {
            if (session.sessionKey() == null || session.dateStart() == null) {
                continue;
            }

            Event parentMeeting = resolveParentMeetingEvent(sport, competition, season, session, meetingEvents);
            UpsertEventResult sessionResult = upsertSessionEvent(sport, competition, season, parentMeeting, session);
            if (sessionResult.created()) {
                sessionsUpserted++;
            }
        }

        return new SyncCounters(venuesUpserted, meetingsUpserted, sessionsUpserted);
    }

    private Sport ensureSport() {
        return sportRepository.findByKey(FORMULA1_KEY).orElseGet(() ->
            sportRepository.save(Sport.builder().key(FORMULA1_KEY).name(FORMULA1_NAME).build())
        );
    }

    private Competition ensureCompetition(Sport sport) {
        return competitionRepository.findBySportIdAndName(sport.getId(), F1_COMPETITION_NAME).orElseGet(() ->
            competitionRepository.save(
                Competition.builder()
                    .sport(sport)
                    .name(F1_COMPETITION_NAME)
                    .kind("series")
                    .region("global")
                    .country(null)
                    .metadata(objectMapper.createObjectNode())
                    .build()
            )
        );
    }

    private Season ensureSeason(Competition competition, int year, List<OpenF1MeetingDto> meetings) {
        Season season = seasonRepository.findByCompetitionIdAndName(competition.getId(), String.valueOf(year)).orElse(null);
        boolean created = false;

        if (season == null) {
            season = Season.builder().competition(competition).name(String.valueOf(year)).build();
            created = true;
        }

        Optional<LocalDate> minStart = meetings.stream()
            .map(OpenF1MeetingDto::dateStart)
            .filter(Objects::nonNull)
            .map(OffsetDateTime::toLocalDate)
            .min(LocalDate::compareTo);

        Optional<LocalDate> maxEnd = meetings.stream()
            .map(OpenF1MeetingDto::dateEnd)
            .filter(Objects::nonNull)
            .map(OffsetDateTime::toLocalDate)
            .max(LocalDate::compareTo);

        boolean changed = false;
        if (minStart.isPresent() && !Objects.equals(season.getStartDate(), minStart.get())) {
            season.setStartDate(minStart.get());
            changed = true;
        }
        if (maxEnd.isPresent() && !Objects.equals(season.getEndDate(), maxEnd.get())) {
            season.setEndDate(maxEnd.get());
            changed = true;
        }

        if (!created && !changed) {
            return season;
        }

        return seasonRepository.save(season);
    }

    private UpsertVenueResult upsertVenue(Sport sport, OpenF1MeetingDto meeting) {
        String venueName = nullSafe(meeting.circuitShortName(), "Unknown Circuit");
        String city = nullSafe(meeting.location(), venueName);

        Venue venue = venueRepository.findBySportIdAndNameAndCity(sport.getId(), venueName, city).orElse(null);
        boolean created = false;

        if (venue == null) {
            venue = Venue.builder()
                .sport(sport)
                .name(venueName)
                .kind("circuit")
                .build();
            created = true;
        }

        JsonNode metadata = buildVenueMetadata(meeting);

        boolean changed = false;
        changed |= setIfChanged(venue.getCity(), city, venue::setCity);
        changed |= setIfChanged(venue.getCountry(), meeting.countryName(), venue::setCountry);
        changed |= setIfChanged(venue.getTimezone(), meeting.gmtOffset(), venue::setTimezone);
        changed |= setIfChanged(venue.getMetadata(), metadata, venue::setMetadata);

        if (!created && !changed) {
            return new UpsertVenueResult(venue, false);
        }

        return new UpsertVenueResult(venueRepository.save(venue), created);
    }

    private UpsertEventResult upsertMeetingEvent(
        Sport sport,
        Competition competition,
        Season season,
        Venue venue,
        OpenF1MeetingDto meeting
    ) {
        String externalId = "meeting:" + meeting.meetingKey();
        Event event = eventRepository.findByExternalProviderAndExternalId(OPENF1_PROVIDER, externalId).orElse(null);
        boolean created = false;

        if (event == null) {
            event = Event.builder()
                .sport(sport)
                .competition(competition)
                .season(season)
                .venue(venue)
                .externalProvider(OPENF1_PROVIDER)
                .externalId(externalId)
                .participantsMode("none")
                .build();
            created = true;
        }

        JsonNode metadata = buildMeetingMetadata(meeting);

        boolean changed = false;
        changed |= setIfChanged(event.getEventType(), "meeting", event::setEventType);
        changed |= setIfChanged(event.getName(), nullSafe(meeting.meetingName(), "Formula 1 Meeting"), event::setName);
        changed |= setIfChanged(event.getStage(), null, event::setStage);
        changed |= setIfChanged(event.getStatus(), statusFor(meeting.dateStart(), meeting.dateEnd()), event::setStatus);
        changed |= setIfChanged(event.getStartTimeUtc(), meeting.dateStart(), event::setStartTimeUtc);
        changed |= setIfChanged(event.getEndTimeUtc(), meeting.dateEnd(), event::setEndTimeUtc);
        changed |= setIfChanged(event.getMetadata(), metadata, event::setMetadata);

        if (!sameEntityId(event.getVenue(), venue)) {
            event.setVenue(venue);
            changed = true;
        }
        if (!sameEntityId(event.getSeason(), season)) {
            event.setSeason(season);
            changed = true;
        }

        if (!created && !changed) {
            return new UpsertEventResult(event, false);
        }

        return new UpsertEventResult(eventRepository.save(event), created);
    }

    private Event resolveParentMeetingEvent(
        Sport sport,
        Competition competition,
        Season season,
        OpenF1SessionDto session,
        Map<Long, Event> cache
    ) {
        if (session.meetingKey() != null && cache.containsKey(session.meetingKey())) {
            return cache.get(session.meetingKey());
        }

        if (session.meetingKey() != null) {
            String externalId = "meeting:" + session.meetingKey();
            Optional<Event> existing = eventRepository.findByExternalProviderAndExternalId(OPENF1_PROVIDER, externalId);
            if (existing.isPresent()) {
                cache.put(session.meetingKey(), existing.get());
                return existing.get();
            }
        }

        Venue fallbackVenue = venueRepository.findBySportIdAndNameAndCity(
            sport.getId(),
            nullSafe(session.circuitShortName(), "Unknown Circuit"),
            nullSafe(session.location(), session.circuitShortName())
        ).orElse(null);

        Event fallback = Event.builder()
            .sport(sport)
            .competition(competition)
            .season(season)
            .venue(fallbackVenue)
            .eventType("meeting")
            .name("Meeting " + nullSafe(session.meetingKey(), 0L))
            .status(statusFor(session.dateStart(), session.dateEnd()))
            .startTimeUtc(session.dateStart())
            .endTimeUtc(session.dateEnd())
            .participantsMode("none")
            .metadata(objectMapper.createObjectNode())
            .externalProvider(OPENF1_PROVIDER)
            .externalId("meeting:" + nullSafe(session.meetingKey(), 0L))
            .build();

        Event saved = eventRepository.save(fallback);
        if (session.meetingKey() != null) {
            cache.put(session.meetingKey(), saved);
        }
        return saved;
    }

    private UpsertEventResult upsertSessionEvent(
        Sport sport,
        Competition competition,
        Season season,
        Event parentMeeting,
        OpenF1SessionDto session
    ) {
        String externalId = "session:" + session.sessionKey();
        Event event = eventRepository.findByExternalProviderAndExternalId(OPENF1_PROVIDER, externalId).orElse(null);
        boolean created = false;

        if (event == null) {
            event = Event.builder()
                .sport(sport)
                .competition(competition)
                .season(season)
                .participantsMode("none")
                .externalProvider(OPENF1_PROVIDER)
                .externalId(externalId)
                .build();
            created = true;
        }

        JsonNode metadata = buildSessionMetadata(session);

        boolean changed = false;
        changed |= setIfChanged(event.getName(), nullSafe(session.sessionName(), "Session " + session.sessionKey()), event::setName);
        changed |= setIfChanged(event.getEventType(), mapEventType(session.sessionType(), session.sessionName()), event::setEventType);
        changed |= setIfChanged(event.getStage(), extractQualifyingStage(session.sessionType(), session.sessionName()), event::setStage);
        changed |= setIfChanged(event.getStatus(), statusFor(session.dateStart(), session.dateEnd()), event::setStatus);
        changed |= setIfChanged(event.getStartTimeUtc(), session.dateStart(), event::setStartTimeUtc);
        changed |= setIfChanged(event.getEndTimeUtc(), session.dateEnd(), event::setEndTimeUtc);
        changed |= setIfChanged(event.getMetadata(), metadata, event::setMetadata);

        if (!sameEntityId(event.getParentEvent(), parentMeeting)) {
            event.setParentEvent(parentMeeting);
            changed = true;
        }
        if (!sameEntityId(event.getVenue(), parentMeeting.getVenue())) {
            event.setVenue(parentMeeting.getVenue());
            changed = true;
        }
        if (!sameEntityId(event.getSeason(), season)) {
            event.setSeason(season);
            changed = true;
        }

        if (!created && !changed) {
            return new UpsertEventResult(event, false);
        }

        return new UpsertEventResult(eventRepository.save(event), created);
    }

    private void upsertVenueLogo(Venue venue, OpenF1MeetingDto meeting) {
        if (venue.getId() == null || meeting.circuitImage() == null || meeting.circuitImage().isBlank()) {
            return;
        }

        boolean exists = mediaAssetRepository.existsByOwnerTypeAndOwnerIdAndAssetTypeAndUrl(
            "venue",
            venue.getId(),
            "logo",
            meeting.circuitImage()
        );

        if (exists) {
            return;
        }

        MediaAsset asset = MediaAsset.builder()
            .ownerType("venue")
            .ownerId(venue.getId())
            .assetType("logo")
            .url(meeting.circuitImage())
            .contentType("image/png")
            .altText(venue.getName() + " circuit logo")
            .sourceProvider(OPENF1_PROVIDER)
            .sourceUrl(meeting.circuitInfoUrl())
            .build();

        mediaAssetRepository.save(asset);
    }

    private String mapEventType(String sessionType, String sessionName) {
        String source = ((sessionType == null ? "" : sessionType) + " " + (sessionName == null ? "" : sessionName)).toLowerCase();
        if (source.contains("sprint")) {
            return "sprint";
        }
        if (source.contains("qualifying")) {
            return "qualifying";
        }
        if (source.contains("race")) {
            return "race";
        }
        if (source.contains("practice") || source.contains("day")) {
            return "practice";
        }
        return "session";
    }

    private String extractQualifyingStage(String sessionType, String sessionName) {
        String merged = ((sessionType == null ? "" : sessionType) + " " + (sessionName == null ? "" : sessionName)).toUpperCase();
        Matcher matcher = QUALIFYING_STAGE_PATTERN.matcher(merged);
        if (matcher.find()) {
            return matcher.group(1).toUpperCase();
        }
        return null;
    }

    private String statusFor(OffsetDateTime start, OffsetDateTime end) {
        OffsetDateTime now = OffsetDateTime.now();
        if (start == null) {
            return "scheduled";
        }
        if (end != null && end.isBefore(now)) {
            return "finished";
        }
        if (start.isBefore(now) && (end == null || end.isAfter(now))) {
            return "live";
        }
        return "scheduled";
    }

    private JsonNode buildVenueMetadata(OpenF1MeetingDto meeting) {
        ObjectNode node = objectMapper.createObjectNode();
        if (meeting.circuitKey() != null) {
            node.put("circuit_key", meeting.circuitKey());
        }
        if (meeting.circuitType() != null) {
            node.put("circuit_type", meeting.circuitType());
        }
        if (meeting.countryCode() != null) {
            node.put("country_code", meeting.countryCode());
        }
        if (meeting.countryKey() != null) {
            node.put("country_key", meeting.countryKey());
        }
        if (meeting.gmtOffset() != null) {
            node.put("gmt_offset", meeting.gmtOffset());
        }
        if (meeting.circuitInfoUrl() != null) {
            node.put("circuit_info_url", meeting.circuitInfoUrl());
        }
        return node;
    }

    private JsonNode buildMeetingMetadata(OpenF1MeetingDto meeting) {
        ObjectNode node = objectMapper.createObjectNode();
        if (meeting.meetingKey() != null) {
            node.put("meeting_key", meeting.meetingKey());
        }
        if (meeting.meetingOfficialName() != null) {
            node.put("meeting_official_name", meeting.meetingOfficialName());
        }
        if (meeting.countryCode() != null) {
            node.put("country_code", meeting.countryCode());
        }
        if (meeting.countryName() != null) {
            node.put("country_name", meeting.countryName());
        }
        if (meeting.year() != null) {
            node.put("year", meeting.year());
        }
        return node;
    }

    private JsonNode buildSessionMetadata(OpenF1SessionDto session) {
        ObjectNode node = objectMapper.createObjectNode();
        if (session.sessionKey() != null) {
            node.put("session_key", session.sessionKey());
        }
        if (session.meetingKey() != null) {
            node.put("meeting_key", session.meetingKey());
        }
        if (session.sessionType() != null) {
            node.put("session_type", session.sessionType());
        }
        if (session.countryCode() != null) {
            node.put("country_code", session.countryCode());
        }
        if (session.countryName() != null) {
            node.put("country_name", session.countryName());
        }
        if (session.gmtOffset() != null) {
            node.put("gmt_offset", session.gmtOffset());
        }
        if (session.year() != null) {
            node.put("year", session.year());
        }
        return node;
    }

    private static String nullSafe(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }

    private static long nullSafe(Long value, long fallback) {
        return value == null ? fallback : value;
    }

    private static <T> boolean setIfChanged(T current, T next, java.util.function.Consumer<T> setter) {
        if (Objects.equals(current, next)) {
            return false;
        }
        setter.accept(next);
        return true;
    }

    private static boolean sameEntityId(Object left, Object right) {
        if (left == null && right == null) {
            return true;
        }
        if (left == null || right == null) {
            return false;
        }
        if (!left.getClass().equals(right.getClass())) {
            return false;
        }
        if (!(left instanceof Event || left instanceof Venue || left instanceof Season)) {
            return Objects.equals(left, right);
        }
        return Objects.equals(readEntityId(left), readEntityId(right));
    }

    private static Long readEntityId(Object entity) {
        if (entity instanceof Event event) {
            return event.getId();
        }
        if (entity instanceof Venue venue) {
            return venue.getId();
        }
        if (entity instanceof Season season) {
            return season.getId();
        }
        return null;
    }

    record SyncCounters(int venuesUpserted, int meetingsUpserted, int sessionsUpserted) {
    }

    private record UpsertVenueResult(Venue venue, boolean created) {
    }

    private record UpsertEventResult(Event event, boolean created) {
    }
}
