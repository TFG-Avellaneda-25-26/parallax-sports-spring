package dev.parallaxsports.external.basketball.service;

import dev.parallaxsports.basketball.BasketballLeague;
import dev.parallaxsports.core.config.properties.ExternalSyncProperties;
import dev.parallaxsports.external.sync.ExternalApiDailySyncJob;
import java.time.LocalDate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class BasketballDailySyncJob implements ExternalApiDailySyncJob {

    private final BasketballSyncService basketballSyncService;
    private final ExternalSyncProperties externalSyncProperties;

    @Override
    public String providerKey() {
        return "basketball";
    }

    @Override
    public void sync(LocalDate executionDate) {
        if (externalSyncProperties.isNbaEnabled()) {
            basketballSyncService.syncSchedulerWindow(BasketballLeague.NBA, executionDate);
        } else {
            log.info("NBA daily sync skipped (app.external-sync.nba-enabled=false)");
        }

        if (externalSyncProperties.isWnbaEnabled()) {
            basketballSyncService.syncSchedulerWindow(BasketballLeague.WNBA, executionDate);
        } else {
            log.info("WNBA daily sync skipped (app.external-sync.wnba-enabled=false)");
        }
    }
}
