package dev.parallaxsports.notification.service;

import dev.parallaxsports.core.config.properties.AlertProperties;
import dev.parallaxsports.notification.model.UserEventAlert;
import dev.parallaxsports.notification.repository.UserEventAlertRepository;
import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
@Slf4j
public class UserEventAlertDispatchScheduler {

    private static final String QUEUED = "queued";
    private static final String FAILED_RETRYABLE = "failed_retryable";
    private static final List<String> CHANNELS = Arrays.asList("telegram", "discord", "email");

    private final UserEventAlertRepository userEventAlertRepository;
    private final AlertStreamPublisher alertStreamPublisher;
    private final AlertProperties alertProperties;

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
            for (UserEventAlert alert : claimedAlerts) {
                try {
                    String streamMessageId = alertStreamPublisher.publish(streamName, alert);
                    alert.setStatus(QUEUED);
                    alert.setStreamName(streamName);
                    alert.setStreamMessageId(streamMessageId);
                    alert.setDispatchedAtUtc(now);
                    alert.setQueuedAtUtc(now);
                    queuedCount++;
                } catch (Exception ex) {
                    alert.setStatus(FAILED_RETRYABLE);
                    alert.setAttempts((alert.getAttempts() == null ? 0 : alert.getAttempts()) + 1);
                    alert.setLastError(ex.getMessage());
                    alert.setNextRetryAtUtc(computeNextRetryAt(now, alert.getAttempts()));
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

    private OffsetDateTime computeNextRetryAt(OffsetDateTime now, Integer attempts) {
        int currentAttempts = attempts == null ? 1 : attempts;
        int[] scheduleMinutes = {1, 3, 10, 30, 120, 480};
        int index = Math.min(Math.max(currentAttempts - 1, 0), scheduleMinutes.length - 1);
        return now.plusMinutes(scheduleMinutes[index]);
    }
}
