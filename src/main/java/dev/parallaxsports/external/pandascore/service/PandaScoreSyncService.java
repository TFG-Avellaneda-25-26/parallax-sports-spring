package dev.parallaxsports.external.pandascore.service;

import dev.parallaxsports.external.pandascore.client.PandaScoreClient;
import dev.parallaxsports.external.pandascore.dto.PandaScoreMatchDto;
import dev.parallaxsports.external.pandascore.service.PandaScoreSyncWriteService.SyncCounters;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class PandaScoreSyncService {

    private final PandaScoreClient pandaScoreClient;
    private final PandaScoreSyncWriteService syncWriteService;

    @Transactional
    public PandaScoreSyncResponse syncVideogame(String videogame, int pages, int perPage) {
        int totalMatches = 0;
        int totalUpserted = 0;

        log.info("Starting PandaScore sync for videogame={} pages={} perPage={}", videogame, pages, perPage);

        for (int page = 1; page <= pages; page++) {
            List<PandaScoreMatchDto> matches = pandaScoreClient.fetchMatches(videogame, page, perPage);

            if (matches.isEmpty()) {
                log.info("No more matches found at page {}, stopping pagination", page);
                break;
            }

            SyncCounters counters = syncWriteService.syncMatches(matches, videogame);
            totalMatches += matches.size();
            totalUpserted += counters.matchesUpserted();

            log.info("PandaScore sync page={} matches={} upserted={}", page, matches.size(), counters.matchesUpserted());
        }

        log.info("PandaScore sync finished videogame={} totalMatches={} totalUpserted={}", videogame, totalMatches, totalUpserted);

        return new PandaScoreSyncResponse(videogame, totalMatches, totalUpserted);
    }

    public record PandaScoreSyncResponse(
        String videogame,
        int matchesFetched,
        int matchesUpserted
    ) {
        // Compat constructor usado por tests antiguos: (videogame, matchesFetched)
        public PandaScoreSyncResponse(String videogame, int matchesFetched) {
            this(videogame, matchesFetched, 0);
        }
    }

    /**
     * Fetch raw matches from PandaScore for a single page (no persistence).
     * Useful for debugging the external API response.
     */
    public List<PandaScoreMatchDto> fetchMatchesRaw(String videogame, int page, int perPage) {
        return pandaScoreClient.fetchMatches(videogame, page, perPage);
    }
}
