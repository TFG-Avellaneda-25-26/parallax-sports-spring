package dev.parallaxsports.external.pandascore.client;

import dev.parallaxsports.core.config.properties.ExternalApiProperties;
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

    // Mapeo de videojuegos a sus prefijos de endpoint en PandaScore
    private static final Map<String, String> VIDEOGAME_ENDPOINTS = Map.of(
        "league-of-legends", "/lol/matches",
        "valorant", "/valorant/matches",
        "dota2", "/dota2/matches",
        "counter-strike", "/csgo/matches"
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
        if (!VIDEOGAME_ENDPOINTS.containsKey(videogame)) {
            log.error("Unsupported videogame: {}. Supported: {}", videogame, VIDEOGAME_ENDPOINTS.keySet());
            return Collections.emptyList();
        }

        // Safety limits
        int maxPerPage = externalApiProperties.getPandascoreMaxPerPage();
        if (perPage > maxPerPage) {
            log.info("perPage capped: {} -> {}", perPage, maxPerPage);
            perPage = maxPerPage;
        }

        // Build URI
        String endpoint = VIDEOGAME_ENDPOINTS.get(videogame);
        String encodedApiKey = URLEncoder.encode(apiKey, StandardCharsets.UTF_8);
        String uri = endpoint 
            + "?page=" + page 
            + "&per_page=" + perPage 
            + "&token=" + encodedApiKey;
        
        String fullUrl = baseUrl + uri;
        log.info("Calling: {}{}?page={}&per_page={}&token=****", baseUrl, endpoint, page, perPage);

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
                    try {
                        Thread.sleep(waitMillis);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        log.error("Interrupted during backoff", ie);
                        return Collections.emptyList();
                    }
                    continue;
                }

                if (attempt < maxRetries) {
                    long waitMillis = Math.max(200L, baseBackoff);
                    log.warn("Transient error, retrying after {}ms (attempt {}/{})", waitMillis, attempt, maxRetries);
                    try {
                        Thread.sleep(waitMillis);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
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
}


