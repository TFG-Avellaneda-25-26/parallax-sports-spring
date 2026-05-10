package dev.parallaxsports.external.pandascore.service;

import dev.parallaxsports.core.config.properties.ExternalApiProperties;
import dev.parallaxsports.external.sync.ExternalApiDailySyncJob;
import java.time.LocalDate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class PandaScoreDailySyncJob implements ExternalApiDailySyncJob {

    private final PandaScoreSyncService pandaScoreSyncService;
    private final ExternalApiProperties externalApiProperties;

    @Override
    public String providerKey() {
        return "pandascore";
    }

    @Override
    public void sync(LocalDate executionDate) {
        log.info("Starting PandaScore daily sync job for date={}", executionDate);

        String[] videogames = {
            "league-of-legends",
            "valorant",
            "dota2",
            "counter-strike"
        };

        int defaultPages = externalApiProperties.getPandascoreDefaultPages();
        int defaultPerPage = externalApiProperties.getPandascoreDefaultPerPage();

        for (String videogame : videogames) {
            try {
                pandaScoreSyncService.syncVideogame(videogame, defaultPages, defaultPerPage);
                log.info("Successfully synced videogame={}", videogame);
            } catch (Exception e) {
                log.error("Error syncing videogame={}", videogame, e);
            }
        }

        log.info("PandaScore daily sync job completed");
    }
}
