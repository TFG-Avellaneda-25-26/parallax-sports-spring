package dev.parallaxsports.external.sync.controller;

import dev.parallaxsports.external.sync.ExternalApiDailyScheduler;
import dev.parallaxsports.external.sync.ExternalSyncExecutionResult;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/sync")
@RequiredArgsConstructor
public class ExternalSyncAdminController {

    private final ExternalApiDailyScheduler externalApiDailyScheduler;

    /*============================================================
      MANUAL SYNC TRIGGER
      Admin endpoint that executes all registered external sync jobs
    ============================================================*/

    /**
     * Triggers daily external synchronization immediately for all providers.
     *
     * @return HTTP 200 with execution summary including discovered, succeeded, and failed jobs
     */
    @PostMapping("/daily/trigger")
    public ResponseEntity<ExternalSyncExecutionResult> triggerDailySyncNow() {
        return ResponseEntity.ok(externalApiDailyScheduler.runDailySyncNow());
    }
}
