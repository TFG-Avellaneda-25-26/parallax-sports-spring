package dev.parallaxsports.notification.service;

import dev.parallaxsports.core.config.properties.AlertProperties;
import dev.parallaxsports.sport.model.Event;
import dev.parallaxsports.sport.repository.EventRepository;
import dev.parallaxsports.notification.client.KtorAlertDispatchClient;
import dev.parallaxsports.notification.model.UserEventAlert;
import dev.parallaxsports.notification.repository.UserEventAlertRepository;
import dev.parallaxsports.notification.service.policy.AlertRetryPolicy;
import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Schedules and dispatches due alerts to the notification microservice.
 *
 * Flow summary:
 * - Claim due alerts per channel in bounded batches.
 * - Publish payload to channel stream.
 * - Optionally fallback to direct HTTP dispatch when Redis publish fails.
 * - Update alert state for queued/failed outcomes.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class UserEventAlertDispatchScheduler {

    private static final String QUEUED = "queued";
    private static final String FAILED_RETRYABLE = "failed_retryable";
    private static final String HTTP_FALLBACK_STREAM = "http-fallback";
    private static final List<String> CHANNELS = Arrays.asList("telegram", "discord", "email");

    private final EventRepository eventRepository;
    private final UserEventAlertRepository userEventAlertRepository;
    private final AlertStreamPublisher alertStreamPublisher;
    private final KtorAlertDispatchClient ktorAlertDispatchClient;
    private final AlertRetryPolicy retryPolicy;
    private final AlertProperties alertProperties;

    /**
        * Dispatches currently due alerts to notification transport.
     *
     * Scanner trigger words: schedule, dispatch, queue, retry, fallback.
     */
    @Scheduled(cron = "${app.alerts.dispatch-cron:0 * * * * *}", zone = "${app.external-sync.zone-id:UTC}")
    @Transactional
    public void dispatchDueAlerts() {
        if (!alertProperties.isDispatchEnabled()) {
            return;
        }

        int batchSize = Math.max(1, alertProperties.getDispatchBatchSize());
        OffsetDateTime now = OffsetDateTime.now();

        int queuedCount = 0;
        int failedCount = 0;

        for (String channel : CHANNELS) {
            String streamName = alertProperties.streamNameForChannel(channel);
            List<Long> claimedIds = userEventAlertRepository.claimDueAlertIdsForChannel(channel, streamName, now, batchSize);
            if (claimedIds.isEmpty()) {
                continue;
            }

            List<UserEventAlert> claimedAlerts = userEventAlertRepository.findAllById(claimedIds);
            Map<Long, Event> eventsById = loadEventsById(claimedAlerts);
            for (UserEventAlert alert : claimedAlerts) {
                try {
                    Event event = eventsById.get(alert.getEventId());
                    String streamMessageId = alertStreamPublisher.publish(streamName, alert, event);
                    alert.setStreamMessageId(streamMessageId);
                    queuedCount++;
                } catch (Exception ex) {
                    if (tryHttpFallbackDispatch(alert, now, ex)) {
                        queuedCount++;
                        continue;
                    }

                    alert.setStatus(FAILED_RETRYABLE);
                    alert.setAttempts((alert.getAttempts() == null ? 0 : alert.getAttempts()) + 1);
                    alert.setLastError(ex.getMessage());
                    alert.setNextRetryAtUtc(retryPolicy.computeNextRetryAt(now, alert.getAttempts()));
                    failedCount++;
                    log.warn(
                        "Alert enqueue failed alertId={} channel={} reason={}",
                        alert.getId(),
                        alert.getChannel(),
                        ex.getMessage()
                    );
                }
            }

            userEventAlertRepository.saveAll(claimedAlerts);
        }

        log.info("Alert enqueue completed queued={} failed={}", queuedCount, failedCount);
    }

    /**
     * Attempts direct HTTP dispatch as a transport fallback.
     *
     * Returns true when fallback dispatch succeeds and alert row has been updated
     * to queued semantics.
     *
     * @param alert claimed alert
     * @param now scheduler timestamp used for queue fields
     * @param publishException Redis publish failure
     * @return true when fallback dispatch succeeds
     */
    private boolean tryHttpFallbackDispatch(UserEventAlert alert, OffsetDateTime now, Exception publishException) {
        if (!alertProperties.isHttpFallbackEnabled()) {
            return false;
        }

        try {
            ktorAlertDispatchClient.dispatch(alert);
            alert.setStreamName(HTTP_FALLBACK_STREAM);
            alert.setStreamMessageId(null);
            log.warn(
                "Alert Redis publish failed; HTTP fallback dispatched alertId={} channel={} reason={}",
                alert.getId(),
                alert.getChannel(),
                publishException.getMessage()
            );
            return true;
        } catch (Exception fallbackException) {
            log.warn(
                "Alert enqueue fallback failed alertId={} channel={} redisReason={} fallbackReason={}",
                alert.getId(),
                alert.getChannel(),
                publishException.getMessage(),
                fallbackException.getMessage()
            );
            return false;
        }
    }

    /**
     * Loads all event rows required for payload enrichment in one batch.
     *
     * @param alerts claimed alerts
     * @return event map keyed by event id
     */
    private Map<Long, Event> loadEventsById(List<UserEventAlert> alerts) {
        Set<Long> eventIds = alerts.stream()
            .map(UserEventAlert::getEventId)
            .filter(Objects::nonNull)
            .collect(Collectors.toSet());

        if (eventIds.isEmpty()) {
            return Map.of();
        }

        return eventRepository.findAllById(eventIds).stream()
            .collect(Collectors.toMap(Event::getId, Function.identity()));
    }

}
