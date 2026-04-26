package dev.parallaxsports.external.pandascore.client;

import dev.parallaxsports.core.config.properties.ExternalApiProperties;
import dev.parallaxsports.external.pandascore.dto.PandaScoreLeagueDto;
import dev.parallaxsports.external.pandascore.dto.PandaScoreMatchDto;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Component
@RequiredArgsConstructor
@Slf4j
public class PandaScoreClient {

    private final RestClient.Builder restClientBuilder;
    private final ExternalApiProperties externalApiProperties;

    // Mapeo de videojuegos a sus prefijos en PandaScore
    private static final Map<String, String> VIDEOGAME_PREFIXES = Map.of(
        "league-of-legends", "/lol",
        "valorant", "/valorant",
        "dota2", "/dota2",
        "counter-strike", "/csgo",
        "overwatch", "/ow"
    );

    public List<PandaScoreMatchDto> fetchMatches(String videogame, int page, int perPage) {
        String apiKey = externalApiProperties.getPandascoreApiKey();

        // Log inicial de diagnóstico
        log.info("=== PandaScore fetchMatches START ===");
        log.info("videogame={} page={} perPage={}", videogame, page, perPage);

        if (apiKey == null || apiKey.isEmpty()) {
            log.error("CRITICAL: PandaScore API key is NOT configured");
            return Collections.emptyList();
        }

        log.info("✓ API key present (length: {})", apiKey.length());

        String baseUrl = externalApiProperties.getPandascoreBaseUrl();
        if (baseUrl == null || baseUrl.isBlank()) {
            baseUrl = "https://api.pandascore.co";
        }
        log.info("✓ Base URL: {}", baseUrl);

        // Validar que el videojuego esté soportado
        if (!VIDEOGAME_PREFIXES.containsKey(videogame)) {
            log.error("Unsupported videogame: {}. Supported: {}", videogame, VIDEOGAME_PREFIXES.keySet());
            return Collections.emptyList();
        }

        // Safety limits
        int maxPerPage = externalApiProperties.getPandascoreMaxPerPage();
        if (perPage > maxPerPage) {
            log.info("perPage capped: {} -> {}", perPage, maxPerPage);
            perPage = maxPerPage;
        }

        // Build URI without date filters (PandaScore API doesn't use filter[begin_at] and filter[end_at])
        String prefix = VIDEOGAME_PREFIXES.get(videogame);
        String uri = prefix + "/matches" 
            + "?page=" + page 
            + "&per_page=" + perPage;
        
        log.info("Calling: {}", baseUrl + uri);

        int maxRetries = externalApiProperties.getPandascoreMaxRetries();
        long baseBackoff = externalApiProperties.getPandascoreBaseBackoffMillis();

        int attempt = 0;
        while (true) {
            attempt++;
            try {
                log.info("Attempt {}/{} to fetch from PandaScore", attempt, maxRetries);
                
                PandaScoreMatchDto[] body = restClientBuilder
                    .baseUrl(baseUrl)
                    .build()
                    .get()
                    .uri(uri)
                    .header("Authorization", "Bearer " + apiKey)
                    .retrieve()
                    .body(PandaScoreMatchDto[].class);

                if (body == null) {
                    log.warn("Response body is null from PandaScore");
                    return Collections.emptyList();
                }

                log.info("✓ Success! Fetched {} matches", body.length);
                log.info("=== PandaScore fetchMatches END ===");
                return Arrays.asList(body);
                
            } catch (RestClientException ex) {
                log.error("RestClientException on attempt {}: {}", attempt, ex.getMessage());
                log.debug("Exception details:", ex);
                
                String msg = ex.getMessage() == null ? "" : ex.getMessage().toLowerCase();
                boolean is429 = msg.contains("429") || msg.contains("too many requests") || msg.contains("rate limit");

                if (is429 && attempt <= maxRetries) {
                    long waitMillis = baseBackoff * (1L << (attempt - 1));
                    log.warn("Rate limit 429 detected, backing off {}ms", waitMillis);
                    if (!sleepQuietly(waitMillis)) {
                        return Collections.emptyList();
                    }
                    continue;
                }

                if (attempt < maxRetries) {
                    long waitMillis = Math.max(200L, baseBackoff);
                    log.warn("Transient error, retrying after {}ms (attempt {}/{})", waitMillis, attempt, maxRetries);
                    if (!sleepQuietly(waitMillis)) {
                        return Collections.emptyList();
                    }
                    continue;
                }

                log.error("Final error after {} attempts: {}", attempt, ex.getMessage(), ex);
                return Collections.emptyList();
                
            } catch (Exception e) {
                log.error("Unexpected error on attempt {}: {}", attempt, e.getMessage(), e);
                return Collections.emptyList();
            }
        }
    }

    public List<PandaScoreLeagueDto> fetchLeagues(String videogame, String tier, int page, int perPage) {
        String apiKey = externalApiProperties.getPandascoreApiKey();

        if (apiKey == null || apiKey.isEmpty()) {
            return Collections.emptyList();
        }

        String baseUrl = externalApiProperties.getPandascoreBaseUrl();
        if (baseUrl == null || baseUrl.isBlank()) {
            baseUrl = "https://api.pandascore.co";
        }

        if (!VIDEOGAME_PREFIXES.containsKey(videogame)) {
            return Collections.emptyList();
        }

        String prefix = VIDEOGAME_PREFIXES.get(videogame);
        StringBuilder uriBuilder = new StringBuilder();
        uriBuilder.append(prefix).append("/leagues?page=").append(page).append("&per_page=").append(perPage);
        
        if (tier != null && !tier.isBlank()) {
            uriBuilder.append("&filter[tier]=").append(tier);
        }

        String uri = uriBuilder.toString();
        int maxRetries = externalApiProperties.getPandascoreMaxRetries();
        long baseBackoff = externalApiProperties.getPandascoreBaseBackoffMillis();

        int attempt = 0;
        while (true) {
            attempt++;
            try {
                PandaScoreLeagueDto[] body = restClientBuilder
                    .baseUrl(baseUrl)
                    .build()
                    .get()
                    .uri(uri)
                    .header("Authorization", "Bearer " + apiKey)
                    .retrieve()
                    .body(PandaScoreLeagueDto[].class);

                if (body == null) {
                    return Collections.emptyList();
                }

                return Arrays.asList(body);
            } catch (RestClientException ex) {
                String msg = ex.getMessage() == null ? "" : ex.getMessage().toLowerCase();
                boolean is429 = msg.contains("429") || msg.contains("too many requests") || msg.contains("rate limit");

                if (is429 && attempt <= maxRetries) {
                    long waitMillis = baseBackoff * (1L << (attempt - 1));
                    if (!sleepQuietly(waitMillis)) {
                        return Collections.emptyList();
                    }
                    continue;
                }

                if (attempt < maxRetries) {
                    long waitMillis = Math.max(200L, baseBackoff);
                    if (!sleepQuietly(waitMillis)) {
                        return Collections.emptyList();
                    }
                    continue;
                }

                return Collections.emptyList();
            } catch (Exception e) {
                return Collections.emptyList();
            }
        }
    }

    public PandaScoreLeagueDto fetchLeague(Long leagueId) {
        String apiKey = externalApiProperties.getPandascoreApiKey();
        if (leagueId == null) {
            return null;
        }
        if (apiKey == null || apiKey.isEmpty()) {
            log.debug("PandaScore API key missing; skipping league lookup for leagueId={}", leagueId);
            return null;
        }

        String baseUrl = externalApiProperties.getPandascoreBaseUrl();
        if (baseUrl == null || baseUrl.isBlank()) {
            baseUrl = "https://api.pandascore.co";
        }

        String uri = "/leagues/" + leagueId;

        int maxRetries = externalApiProperties.getPandascoreMaxRetries();
        long baseBackoff = externalApiProperties.getPandascoreBaseBackoffMillis();

        int attempt = 0;
        while (true) {
            attempt++;
            try {
                log.debug("Fetching PandaScore league details for leagueId={} attempt {}/{}", leagueId, attempt, maxRetries);

                return restClientBuilder
                    .baseUrl(baseUrl)
                    .build()
                    .get()
                    .uri(uri)
                    .header("Authorization", "Bearer " + apiKey)
                    .retrieve()
                    .body(PandaScoreLeagueDto.class);
            } catch (RestClientException ex) {
                log.debug("PandaScore league lookup failed on attempt {} for leagueId={}: {}", attempt, leagueId, ex.getMessage());

                String msg = ex.getMessage() == null ? "" : ex.getMessage().toLowerCase();
                boolean is429 = msg.contains("429") || msg.contains("too many requests") || msg.contains("rate limit");

                if (is429 && attempt <= maxRetries) {
                    long waitMillis = baseBackoff * (1L << (attempt - 1));
                    if (!sleepQuietly(waitMillis)) {
                        return null;
                    }
                    continue;
                }

                if (attempt < maxRetries) {
                    long waitMillis = Math.max(200L, baseBackoff);
                    if (!sleepQuietly(waitMillis)) {
                        return null;
                    }
                    continue;
                }

                return null;
            } catch (Exception e) {
                log.debug("Unexpected error fetching PandaScore leagueId={}: {}", leagueId, e.getMessage());
                return null;
            }
        }
    }

    private boolean sleepQuietly(long waitMillis) {
        try {
            Thread.sleep(waitMillis);
            return true;
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
            log.error("Interrupted during backoff", ie);
            return false;
        }
    }
}


