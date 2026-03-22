package dev.parallaxsports.external.sync;

import java.time.LocalDate;

/**
 * Summary contract for one external sync execution cycle.
 *
 * @param executionDate scheduler date used to run provider jobs
 * @param jobsDiscovered total provider jobs available in the application context
 * @param jobsSucceeded provider jobs completed without exception
 * @param jobsFailed provider jobs that raised an exception
 */
public record ExternalSyncExecutionResult(
    LocalDate executionDate,
    int jobsDiscovered,
    int jobsSucceeded,
    int jobsFailed
) {
}
