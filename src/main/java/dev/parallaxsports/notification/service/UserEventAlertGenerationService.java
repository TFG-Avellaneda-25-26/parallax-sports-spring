package dev.parallaxsports.notification.service;

import dev.parallaxsports.core.config.properties.AlertProperties;
import dev.parallaxsports.follow.model.UserSportFollow;
import dev.parallaxsports.follow.model.UserSportNotificationChannel;
import dev.parallaxsports.follow.model.UserSportSettings;
import dev.parallaxsports.sport.repository.EventEntryRepository;
import dev.parallaxsports.follow.repository.UserSportFollowRepository;
import dev.parallaxsports.follow.repository.UserSportNotificationChannelRepository;
import dev.parallaxsports.follow.repository.UserSportSettingsRepository;
import dev.parallaxsports.sport.model.Event;
import dev.parallaxsports.notification.model.UserEventAlert;
import dev.parallaxsports.notification.repository.UserEventAlertRepository;
import dev.parallaxsports.user.repository.UserRepository;
import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Generates or updates per-user alert rows for upcoming events.
 *
 * Generation combines:
 * - user sport settings,
 * - explicit follow targets,
 * - enabled notification channels,
 * and then computes one idempotent alert record per effective user/channel/lead-time tuple.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class UserEventAlertGenerationService {

    private static final String SCHEDULED = "scheduled";
    private static final String WAITING_ARTIFACT = "waiting_artifact";
    private static final Set<String> UPDATABLE_STATUSES = Set.of(SCHEDULED, WAITING_ARTIFACT);

    private final UserSportSettingsRepository userSportSettingsRepository;
    private final UserSportFollowRepository userSportFollowRepository;
    private final UserSportNotificationChannelRepository userSportNotificationChannelRepository;
    private final EventEntryRepository eventEntryRepository;
    private final UserEventAlertRepository userEventAlertRepository;
    private final UserRepository userRepository;
    private final AlertProperties alertProperties;

    /**
     * Creates/updates alerts for incoming event candidates.
     *
     * Scanner trigger words: generate, upsert, eligible users, lead time.
     *
     * @param events event candidates that may produce alerts
     */
    @Transactional
    public void generateForEvents(List<Event> events) {
        if (!alertProperties.isGenerationEnabled() || events.isEmpty()) {
            return;
        }

        OffsetDateTime now = OffsetDateTime.now();
        Map<Long, List<UserSportSettings>> settingsBySportId = new HashMap<>();
        Map<Long, List<UserSportFollow>> followsBySportId = new HashMap<>();
        Map<Long, List<UserSportNotificationChannel>> channelsBySportId = new HashMap<>();

        int generatedCount = 0;
        for (Event event : events) {
            if (event.getSport() == null || event.getSport().getId() == null || event.getStartTimeUtc() == null) {
                continue;
            }

            Long sportId = event.getSport().getId();
            List<UserSportSettings> sportSettings = settingsBySportId.computeIfAbsent(
                sportId,
                userSportSettingsRepository::findByIdSportId
            );
            List<UserSportFollow> sportFollows = followsBySportId.computeIfAbsent(
                sportId,
                userSportFollowRepository::findBySportIdAndNotifyTrue
            );
            List<UserSportNotificationChannel> sportChannels = channelsBySportId.computeIfAbsent(
                sportId,
                userSportNotificationChannelRepository::findByIdSportIdAndEnabledTrue
            );

            Set<Long> eligibleUserIds = resolveEligibleUsers(event, sportSettings, sportFollows);
            if (eligibleUserIds.isEmpty()) {
                continue;
            }

            for (Long userId : eligibleUserIds) {
                List<UserSportNotificationChannel> userChannels = sportChannels.stream()
                    .filter(ch -> ch.getId() != null && Objects.equals(ch.getId().getUserId(), userId))
                    .toList();

                if (userChannels.isEmpty()) {
                    String fallbackChannel = normalizeChannel(alertProperties.getDefaultChannel());
                    int fallbackLead = Math.max(1, alertProperties.getDefaultLeadTimeMinutes());
                    OffsetDateTime fallbackSendAt = event.getStartTimeUtc().minusMinutes(fallbackLead);
                    if (!fallbackSendAt.isBefore(now)) {
                        upsertAlert(userId, event.getId(), fallbackChannel, fallbackLead, fallbackSendAt);
                        generatedCount++;
                    }
                    continue;
                }

                for (UserSportNotificationChannel channelSetting : userChannels) {
                    String channel = normalizeChannel(channelSetting.getId().getChannel());
                    int leadTimeMinutes = Math.max(1, channelSetting.getDefaultLeadTimeMinutes());
                    OffsetDateTime sendAtUtc = event.getStartTimeUtc().minusMinutes(leadTimeMinutes);
                    if (sendAtUtc.isBefore(now)) {
                        continue;
                    }
                    upsertAlert(userId, event.getId(), channel, leadTimeMinutes, sendAtUtc);
                    generatedCount++;
                }
            }
        }

        log.info("Alert generation completed events={} generatedOrUpdated={}", events.size(), generatedCount);
    }

    /**
     * Computes users eligible to receive alerts for one event.
     *
     * Eligibility is the union of follow-all settings and explicit follows that
     * match target + event type filters.
     *
     * @param event source event
     * @param sportSettings settings rows for sport
     * @param sportFollows follow rows for sport
     * @return set of eligible user ids
     */
    private Set<Long> resolveEligibleUsers(
        Event event,
        List<UserSportSettings> sportSettings,
        List<UserSportFollow> sportFollows
    ) {
        Set<Long> eligibleUserIds = new HashSet<>();
        String eventType = event.getEventType();

        for (UserSportSettings settings : sportSettings) {
            if (!settings.isNotifyDefault() || !settings.isFollowAll()) {
                continue;
            }
            if (!matchesEventType(eventType, settings.getEventTypeFilter())) {
                continue;
            }
            if (settings.getId() != null && settings.getId().getUserId() != null) {
                eligibleUserIds.add(settings.getId().getUserId());
            }
        }

        for (UserSportFollow follow : sportFollows) {
            if (follow.getUser() == null || follow.getUser().getId() == null) {
                continue;
            }
            if (!matchesTarget(event, follow)) {
                continue;
            }
            if (!matchesEventType(eventType, follow.getEventTypeFilter())) {
                continue;
            }
            eligibleUserIds.add(follow.getUser().getId());
        }

        return eligibleUserIds;
    }

    /**
     * Matches event target against one follow target.
     *
     * @param event source event
     * @param follow follow row
     * @return true when follow target matches event
     */
    private boolean matchesTarget(Event event, UserSportFollow follow) {
        String followType = follow.getFollowType();
        if ("competition".equalsIgnoreCase(followType)) {
            Long eventCompetitionId = event.getCompetition() == null ? null : event.getCompetition().getId();
            return Objects.equals(eventCompetitionId, follow.getCompetitionId());
        }
        if ("participant".equalsIgnoreCase(followType)) {
            if (event.getId() == null || follow.getParticipantId() == null) {
                return false;
            }
            return eventEntryRepository.existsByIdEventIdAndIdParticipantId(event.getId(), follow.getParticipantId());
        }
        return false;
    }

    /**
     * Applies optional event-type filtering.
     *
     * @param eventType event type from source event
     * @param filters configured filter list
     * @return true when event type is accepted
     */
    private boolean matchesEventType(String eventType, List<String> filters) {
        if (filters == null || filters.isEmpty()) {
            return true;
        }

        if (eventType == null) {
            return false;
        }

        return filters.stream().anyMatch(f -> f != null && f.equalsIgnoreCase(eventType));
    }

    /**
     * Upserts one alert row for a user/event/channel/lead-time tuple.
     *
     * @param userId target user
     * @param eventId source event id
     * @param channel channel key
     * @param leadTimeMinutes lead time before event
     * @param sendAtUtc computed schedule timestamp
     */
    private void upsertAlert(Long userId, Long eventId, String channel, int leadTimeMinutes, OffsetDateTime sendAtUtc) {
        boolean artifactRequired = "discord".equalsIgnoreCase(channel);
        String computedStatus = artifactRequired ? WAITING_ARTIFACT : SCHEDULED;
        String idempotencyKey = userId + ":" + eventId + ":" + channel + ":" + leadTimeMinutes;

        userEventAlertRepository
            .findByUser_IdAndEventIdAndChannelAndLeadTimeMinutes(userId, eventId, channel, leadTimeMinutes)
            .ifPresentOrElse(existing -> {
                if (!UPDATABLE_STATUSES.contains(existing.getStatus())) {
                    return;
                }
                existing.setSendAtUtc(sendAtUtc);
                existing.setIdempotencyKey(idempotencyKey);
                existing.setArtifactRequired(artifactRequired);
                if (artifactRequired && existing.getArtifactId() == null) {
                    existing.setStatus(WAITING_ARTIFACT);
                } else {
                    existing.setStatus(computedStatus);
                }
                userEventAlertRepository.save(existing);
            }, () -> {
                UserEventAlert alert = UserEventAlert.builder()
                    .user(userRepository.getReferenceById(userId))
                    .eventId(eventId)
                    .channel(channel)
                    .leadTimeMinutes(leadTimeMinutes)
                    .sendAtUtc(sendAtUtc)
                    .idempotencyKey(idempotencyKey)
                    .status(computedStatus)
                    .artifactRequired(artifactRequired)
                    .build();
                userEventAlertRepository.save(alert);
            });
    }

    /**
     * Normalizes configured channel values to supported channel keys.
     *
     * @param configuredChannel raw channel value
     * @return normalized channel, defaulting to telegram
     */
    private static final Set<String> SUPPORTED_CHANNELS = Set.of("telegram", "discord", "email");

    private String normalizeChannel(String configuredChannel) {
        if (configuredChannel == null || configuredChannel.isBlank()) {
            return "telegram";
        }

        String normalized = configuredChannel.toLowerCase();
        return SUPPORTED_CHANNELS.contains(normalized) ? normalized : "telegram";
    }
}
