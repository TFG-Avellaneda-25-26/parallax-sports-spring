package dev.parallaxsports.external.formula1.service;

import dev.parallaxsports.external.formula1.dto.OpenF1MeetingDto;
import dev.parallaxsports.external.formula1.dto.OpenF1SessionDto;
import dev.parallaxsports.sport.model.Competition;
import dev.parallaxsports.sport.model.Event;
import dev.parallaxsports.sport.model.MediaAsset;
import dev.parallaxsports.sport.model.Season;
import dev.parallaxsports.sport.model.Sport;
import dev.parallaxsports.sport.model.Venue;
import dev.parallaxsports.sport.repository.CompetitionRepository;
import dev.parallaxsports.sport.repository.EventRepository;
import dev.parallaxsports.sport.repository.MediaAssetRepository;
import dev.parallaxsports.sport.repository.SeasonRepository;
import dev.parallaxsports.sport.repository.SportRepository;
import dev.parallaxsports.sport.repository.VenueRepository;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
class Formula1SyncWriteService {

    private static final String OPENF1_PROVIDER = "openf1";
    private static final String FORMULA1_KEY = "formula1";
    private static final String FORMULA1_NAME = "Formula 1";
    private static final String F1_COMPETITION_NAME = "Formula 1 World Championship";

    private final SportRepository sportRepository;
    private final CompetitionRepository competitionRepository;
    private final SeasonRepository seasonRepository;
    private final VenueRepository venueRepository;
    private final EventRepository eventRepository;
    private final MediaAssetRepository mediaAssetRepository;


    /**
     * Synchronizes a Formula 1 season and returns counters plus processed session event ids.
     *
     * <p>The returned ids represent session events seen during this sync run (created or updated)
     * and are used by the caller to generate alerts only for the affected sessions.</p>
     */
        /*============================================================
            OPENF1 TO DATABASE SYNC (ONE SEASON)
            Upsert reference data plus meeting/session events
        ============================================================*/

        /**
         * Synchronizes meetings and sessions for a season year into normalized domain entities.
         *
         * @param year target season year
         * @param meetings provider meetings used for venues and meeting events
         * @param sessions provider sessions used for child events under meetings
         * @return counters with number of created venues, meetings, and sessions
         */

  SyncCounters syncSeason(int year, List<OpenF1MeetingDto> meetings, List<OpenF1SessionDto> sessions) {
        Sport sport = ensureSport();
        Competition competition = ensureCompetition(sport);
        Season season = ensureSeason(competition, year, meetings);

        Map<Long, Event> meetingEvents = new HashMap<>();

        int venuesUpserted = 0;
        int meetingsUpserted = 0;
        int sessionsUpserted = 0;
        List<Long> processedSessionEventIds = new ArrayList<>();

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
            if (sessionResult.event().getId() != null) {
                processedSessionEventIds.add(sessionResult.event().getId());
            }
        }

        return new SyncCounters(venuesUpserted, meetingsUpserted, sessionsUpserted, processedSessionEventIds);
    }

        /*============================================================
            ENSURE MASTER DATA ROWS
            Resolve or create sport, competition, and season
        ============================================================*/

    /**
     * Resolves the Formula 1 sport row, creating it on first sync.
     *
     * @return persistent sport entity keyed as formula1
     */
    private Sport ensureSport() {
        return sportRepository.findByKey(FORMULA1_KEY).orElseGet(() ->
            sportRepository.save(Sport.builder().key(FORMULA1_KEY).name(FORMULA1_NAME).build())
        );
    }

    /**
     * Resolves the Formula 1 world championship competition, creating it if absent.
     *
     * @param sport owning sport entity
     * @return persistent competition entity used as parent for synced seasons and events
     */
    private Competition ensureCompetition(Sport sport) {
        return competitionRepository.findBySportIdAndName(sport.getId(), F1_COMPETITION_NAME).orElseGet(() ->
            competitionRepository.save(
                Competition.builder()
                    .sport(sport)
                    .name(F1_COMPETITION_NAME)
                    .kind("series")
                    .region("global")
                    .country(null)
                    .build()
            )
        );
    }

    /**
     * Resolves the season row for the target year and aligns season date bounds from meeting data.
     *
     * @param competition parent competition for the season
     * @param year season year
     * @param meetings source meetings used to derive min start and max end dates
     * @return existing or newly persisted season entity
     */
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

        /*============================================================
            MEETING UPSERT FLOW
            Upsert venues, meeting events, and parent references
        ============================================================*/

        /**
         * Upserts a circuit venue from meeting data and updates mutable venue fields.
         *
         * @param sport owning sport entity
         * @param meeting source meeting payload
         * @return venue and creation flag indicating whether a new row was inserted
         */
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

        boolean changed = false;
        changed |= setIfChanged(venue.getCity(), city, venue::setCity);
        changed |= setIfChanged(venue.getCountry(), meeting.countryName(), venue::setCountry);
        changed |= setIfChanged(venue.getTimezone(), meeting.gmtOffset(), venue::setTimezone);

        if (!created && !changed) {
            return new UpsertVenueResult(venue, false);
        }

        return new UpsertVenueResult(venueRepository.save(venue), created);
    }

    /**
     * Upserts the meeting-level event associated with a provider meeting key.
     *
     * @param sport owning sport entity
     * @param competition parent competition
     * @param season parent season
     * @param venue resolved venue for the meeting
     * @param meeting source meeting payload
     * @return event and creation flag indicating whether a new row was inserted
     */
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

        boolean changed = false;
        changed |= setIfChanged(event.getEventType(), "meeting", event::setEventType);
        changed |= setIfChanged(event.getName(), nullSafe(meeting.meetingName(), "Formula 1 Meeting"), event::setName);
        changed |= setIfChanged(event.getStatus(), statusFor(meeting.dateStart(), meeting.dateEnd()), event::setStatus);
        changed |= setIfChanged(event.getStartTimeUtc(), meeting.dateStart(), event::setStartTimeUtc);
        changed |= setIfChanged(event.getEndTimeUtc(), meeting.dateEnd(), event::setEndTimeUtc);

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

    /**
     * Resolves the parent meeting event for a session using cache, DB lookup, or fallback creation.
     *
     * @param sport owning sport entity
     * @param competition parent competition
     * @param season parent season
     * @param session source session payload
     * @param cache per-run cache of meeting key to meeting event
     * @return meeting event that should be assigned as parent for the session
     */
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
            .externalProvider(OPENF1_PROVIDER)
            .externalId("meeting:" + nullSafe(session.meetingKey(), 0L))
            .build();

        Event saved = eventRepository.save(fallback);
        if (session.meetingKey() != null) {
            cache.put(session.meetingKey(), saved);
        }
        return saved;
    }

        /*============================================================
            SESSION UPSERT FLOW
            Upsert child session events and align hierarchy links
        ============================================================*/

        /**
         * Upserts one session event and links it to its parent meeting and season context.
         *
         * @param sport owning sport entity
         * @param competition parent competition
         * @param season parent season
         * @param parentMeeting resolved parent meeting event
         * @param session source session payload
         * @return event and creation flag indicating whether a new row was inserted
         */
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

        boolean changed = false;
        changed |= setIfChanged(event.getName(), nullSafe(session.sessionName(), "Session " + session.sessionKey()), event::setName);
        changed |= setIfChanged(event.getEventType(), mapEventType(session.sessionType(), session.sessionName()), event::setEventType);
        changed |= setIfChanged(event.getStatus(), statusFor(session.dateStart(), session.dateEnd()), event::setStatus);
        changed |= setIfChanged(event.getStartTimeUtc(), session.dateStart(), event::setStartTimeUtc);
        changed |= setIfChanged(event.getEndTimeUtc(), session.dateEnd(), event::setEndTimeUtc);

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

        /*============================================================
            MEDIA ENRICHMENT
            Persist circuit logo assets attached to venue entities
        ============================================================*/

        /**
         * Creates a venue logo media asset when the provider supplies a new circuit image URL.
         *
         * @param venue target venue entity
         * @param meeting source meeting payload that may contain image metadata
         */
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

        /*============================================================
            DOMAIN MAPPING RULES
            Translate provider text into normalized event typing and status
        ============================================================*/

        /**
         * Maps provider session labels into normalized event types used by the API.
         *
         * @param sessionType provider session type text
         * @param sessionName provider session display name
         * @return normalized event type value
         */
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

    /**
     * Derives lifecycle status from event date bounds relative to current time.
     *
     * @param start event start timestamp
     * @param end event end timestamp
     * @return scheduled, live, or finished status value
     */
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

    /*============================================================
      GENERIC HELPERS
      Null handling, change detection, and same-entity checks
    ============================================================*/

    /**
     * Returns the fallback when value is null or blank.
     *
     * @param value source text
     * @param fallback replacement text when source is empty
     * @return non-blank value
     */
    private static String nullSafe(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }

    /**
     * Returns the fallback when value is null.
     *
     * @param value source number
     * @param fallback replacement number
     * @return source or fallback
     */
    private static long nullSafe(Long value, long fallback) {
        return value == null ? fallback : value;
    }

    /**
     * Updates a field only when the value changed.
     *
     * @param current current value
     * @param next candidate value
     * @param setter mutator to call when changed
     * @return true when an update was applied
     * @param <T> compared value type
     */
    private static <T> boolean setIfChanged(T current, T next, java.util.function.Consumer<T> setter) {
        if (Objects.equals(current, next)) {
            return false;
        }
        setter.accept(next);
        return true;
    }

    /**
     * Compares supported entities by id to avoid false positives from detached object instances.
     *
     * @param left first entity candidate
     * @param right second entity candidate
     * @return true when both represent the same persisted entity or both are null
     */
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

    /**
     * Reads entity id for supported types used in sync association checks.
     *
     * @param entity supported entity instance
     * @return entity id or null when unsupported
     */
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

    record SyncCounters(int venuesUpserted, int meetingsUpserted, int sessionsUpserted, List<Long> processedSessionEventIds) {
    }

    private record UpsertVenueResult(Venue venue, boolean created) {
    }

    private record UpsertEventResult(Event event, boolean created) {
    }
}
