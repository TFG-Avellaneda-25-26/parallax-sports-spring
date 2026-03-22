package dev.parallaxsports.external.formula1.service;

import dev.parallaxsports.core.config.properties.ExternalSyncProperties;
import dev.parallaxsports.external.sync.ExternalApiDailySyncJob;
import java.time.LocalDate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class Formula1DailySyncJob implements ExternalApiDailySyncJob {

    private final Formula1SyncService formula1SyncService;
    private final ExternalSyncProperties externalSyncProperties;

        /*============================================================
            DAILY FORMULA 1 SYNC JOB
            Called by scheduler to sync configured year window
        ============================================================*/

        /**
         * Exposes the provider key used by the shared external sync scheduler.
         *
         * @return stable provider identifier for Formula 1 sync logs and job routing
         */
    @Override
    public String providerKey() {
        return "formula1";
    }

    /**
         * Synchronizes Formula 1 seasons for the configured year window around the execution date.
         *
         * @param executionDate scheduler-provided date that anchors the year window
         */
    @Override
    public void sync(LocalDate executionDate) {
        int baseYear = executionDate.getYear();
        int yearsBack = Math.max(externalSyncProperties.getYearsBack(), 0);
        int yearsForward = Math.max(externalSyncProperties.getYearsForward(), 0);

        int startYear = baseYear - yearsBack;
        int endYear = baseYear + yearsForward;

        for (int year = startYear; year <= endYear; year++) {
            formula1SyncService.syncYear(year);
        }

        log.info(
            "Formula1 daily sync executed executionDate={} startYear={} endYear={}",
            executionDate,
            startYear,
            endYear
        );
    }
}
