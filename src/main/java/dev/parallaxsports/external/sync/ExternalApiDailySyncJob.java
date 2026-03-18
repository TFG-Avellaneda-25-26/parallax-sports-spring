package dev.parallaxsports.external.sync;

import java.time.LocalDate;

/**
 * Contract for provider-specific daily synchronization jobs.
 */
public interface ExternalApiDailySyncJob {

    /**
     * Returns the stable provider identifier used in logs and summaries.
     *
     * @return provider key (for example, formula1)
     */
    String providerKey();

    /**
     * Synchronizes provider data for the scheduler execution date.
     *
     * @param executionDate date resolved from scheduler zone configuration
     */
    void sync(LocalDate executionDate);
}
