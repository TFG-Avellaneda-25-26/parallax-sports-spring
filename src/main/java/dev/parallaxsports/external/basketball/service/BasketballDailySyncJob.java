package dev.parallaxsports.external.basketball.service;

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
        if (!externalSyncProperties.isBasketballEnabled()) {
            log.info("Basketball daily sync skipped because app.external-sync.basketball-enabled=false");
            return;
        }

        basketballSyncService.syncSchedulerWindow(executionDate);
    }
}
