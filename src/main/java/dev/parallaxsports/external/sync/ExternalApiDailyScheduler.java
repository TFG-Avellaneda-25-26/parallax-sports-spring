package dev.parallaxsports.external.sync;

import dev.parallaxsports.core.config.properties.ExternalSyncProperties;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
@ConditionalOnProperty(prefix = "app.external-sync", name = "enabled", havingValue = "true", matchIfMissing = true)
public class ExternalApiDailyScheduler {

    private final List<ExternalApiDailySyncJob> jobs;
    private final ExternalSyncProperties externalSyncProperties;

        /*============================================================
            SCHEDULED ENTRYPOINT
            Cron-driven execution using configured zone and time window
        ============================================================*/

        /**
         * Triggers daily external synchronization using the configured cron schedule.
         *
         * @return no value; side effect is provider synchronization and structured logs per provider
         */
    @Scheduled(cron = "${app.external-sync.daily-cron:0 30 0 * * *}", zone = "${app.external-sync.zone-id:UTC}")
    public void runDailySync() {
        executeDailySync();
    }

        /*============================================================
            MANUAL ENTRYPOINT
            Admin-triggered execution returning an execution summary
        ============================================================*/

        /**
         * Executes the same daily sync flow immediately and returns a run summary.
         *
         * @return execution result with date and per-provider success/failure counts
         */
    public ExternalSyncExecutionResult runDailySyncNow() {
        return executeDailySync();
    }

        /*============================================================
            INTERNAL ORCHESTRATION
            Shared flow used by scheduled and manual entrypoints
        ============================================================*/

        /**
         * Resolves execution date, runs each registered provider job, and aggregates outcomes.
         *
         * @return execution summary for observability and API responses
         */
    private ExternalSyncExecutionResult executeDailySync() {
        ZoneId zoneId = ZoneId.of(externalSyncProperties.getZoneId());
        LocalDate executionDate = LocalDate.now(zoneId);

        if (jobs.isEmpty()) {
            return new ExternalSyncExecutionResult(executionDate, 0, 0, 0);
        }

        int succeeded = 0;
        int failed = 0;

        for (ExternalApiDailySyncJob job : jobs) {
            try {
                job.sync(executionDate);
                succeeded++;
                log.info("Daily external sync finished provider={} executionDate={}", job.providerKey(), executionDate);
            } catch (Exception ex) {
                failed++;
                log.error(
                    "Daily external sync failed provider={} executionDate={} reason={}",
                    job.providerKey(),
                    executionDate,
                    ex.getMessage(),
                    ex
                );
            }
        }

        return new ExternalSyncExecutionResult(executionDate, jobs.size(), succeeded, failed);
    }
}
