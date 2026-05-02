package dev.parallaxsports.sport.event.service;

import dev.parallaxsports.sport.event.dto.EventFeedResponse;
import dev.parallaxsports.sport.event.dto.EventResponse;
import dev.parallaxsports.sport.event.dto.EventResponse.ParticipantEntry;
import dev.parallaxsports.sport.model.Competition;
import dev.parallaxsports.sport.model.Event;
import dev.parallaxsports.sport.model.EventEntry;
import dev.parallaxsports.sport.model.MediaAsset;
import dev.parallaxsports.sport.model.Participant;
import dev.parallaxsports.sport.model.Sport;
import dev.parallaxsports.sport.model.Venue;
import dev.parallaxsports.sport.repository.EventEntryRepository;
import dev.parallaxsports.sport.repository.EventRepository;
import dev.parallaxsports.sport.repository.MediaAssetRepository;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class EventFeedService {

    public static final String CACHE_PREFIX = "events:feed:";
    private static final Duration CACHE_TTL = Duration.ofHours(6);

    private final EventRepository eventRepository;
    private final EventEntryRepository eventEntryRepository;
    private final MediaAssetRepository mediaAssetRepository;
    private final RedisTemplate<String, Object> redisTemplate;

    @Transactional(readOnly = true)
    public EventFeedResponse getFeed(OffsetDateTime from, OffsetDateTime to, Long afterId, int size) {
        String cacheKey = CACHE_PREFIX + from.toLocalDate() + ":" + to.toLocalDate() + ":" + afterId + ":" + size;

        try {
            Object cached = redisTemplate.opsForValue().get(cacheKey);
            if (cached instanceof EventFeedResponse response) {
                return response;
            }
        } catch (Exception e) {
            log.warn("Redis cache read failed key='{}': {}", cacheKey, e.getMessage());
        }

        Pageable limit = PageRequest.ofSize(size + 1);
        List<Event> events;
        if (afterId == null) {
            events = eventRepository.findAllByTimeBetween(from, to, limit);
        } else {
            if (!eventRepository.existsById(afterId)) {
                throw new IllegalArgumentException("Invalid cursor: event id " + afterId + " not found");
            }
            events = eventRepository.findAllByTimeBetweenAfterCursor(from, to, afterId, limit);
        }

        boolean hasMore = events.size() > size;
        if (hasMore) {
            events = events.subList(0, size);
        }

        if (events.isEmpty()) {
            return new EventFeedResponse(List.of(), null, false);
        }

        Set<Long> eventIds = events.stream().map(Event::getId).collect(Collectors.toSet());

        Map<Long, List<EventEntry>> entriesByEvent = eventEntryRepository
            .findWithParticipantByEventIdIn(eventIds).stream()
            .collect(Collectors.groupingBy(ee -> ee.getId().getEventId()));

        List<Long> participantIds = entriesByEvent.values().stream()
            .flatMap(Collection::stream)
            .map(ee -> ee.getParticipant().getId())
            .distinct()
            .toList();
        Map<Long, String> participantLogos = loadParticipantLogos(participantIds);

        List<EventResponse> items = events.stream()
            .map(e -> toEventResponse(e, entriesByEvent, participantLogos))
            .toList();

        Long nextCursor = hasMore ? events.getLast().getId() : null;
        EventFeedResponse feed = new EventFeedResponse(items, nextCursor, hasMore);

        try {
            redisTemplate.opsForValue().set(cacheKey, feed, CACHE_TTL);
        } catch (Exception e) {
            log.warn("Redis cache write failed key='{}': {}", cacheKey, e.getMessage());
        }

        return feed;
    }

    private Map<Long, String> loadParticipantLogos(List<Long> participantIds) {
        if (participantIds.isEmpty()) {
            return Map.of();
        }

        List<MediaAsset> logos = mediaAssetRepository
            .findByOwnerTypeAndOwnerIdInAndAssetTypeOrderByOwnerIdAscIdDesc(
                "participant", participantIds, "logo"
            );

        Map<Long, String> latestByParticipant = new HashMap<>();
        for (MediaAsset logo : logos) {
            latestByParticipant.putIfAbsent(logo.getOwnerId(), logo.getUrl());
        }
        return latestByParticipant;
    }

    private EventResponse toEventResponse(
        Event event,
        Map<Long, List<EventEntry>> entriesByEvent,
        Map<Long, String> participantLogos
    ) {
        Sport sport = event.getSport();
        Competition competition = event.getCompetition();
        Venue venue = event.getVenue();

        List<EventEntry> entries = entriesByEvent.getOrDefault(event.getId(), List.of());

        List<ParticipantEntry> participants = entries.stream()
            .map(ee -> {
                Participant p = ee.getParticipant();
                return new ParticipantEntry(p.getId(), p.getName(), p.getShortName(), ee.getSide());
            })
            .toList();

        List<String> logos = entries.stream()
            .map(ee -> participantLogos.get(ee.getParticipant().getId()))
            .filter(Objects::nonNull)
            .toList();

        return new EventResponse(
            event.getId(),
            sport.getKey(),
            sport.getName(),
            event.getEventType(),
            event.getName(),
            event.getStatus(),
            event.getStartTimeUtc(),
            event.getEndTimeUtc(),
            competition != null ? competition.getName() : null,
            venue != null ? venue.getName() : null,
            participants,
            logos
        );
    }
}
