package dev.parallaxsports.external.pandascore.client;

import dev.parallaxsports.core.config.properties.ExternalApiProperties;
import dev.parallaxsports.external.pandascore.dto.PandaScoreMatchDto;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
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

    private static final String MATCHES_ENDPOINT = "/matches";

    public List<PandaScoreMatchDto> fetchMatches(String videogame, int page, int perPage) {
        try {
            String apiKey = externalApiProperties.getPandascoreApiKey();

            if (apiKey == null || apiKey.isEmpty()) {
                log.error("PandaScore API key is not configured");
                return Collections.emptyList();
            }

            String baseUrl = externalApiProperties.getPandascoreBaseUrl();
            if (baseUrl == null || baseUrl.isBlank()) {
                baseUrl = "https://api.pandascore.co"; // fallback
            }

            // Safety limits
            int maxPerPage = externalApiProperties.getPandascoreMaxPerPage();
            if (perPage > maxPerPage) {
                log.warn("Requested perPage={} is greater than maxPerPage={}, reducing to max", perPage, maxPerPage);
                perPage = maxPerPage;
            }

            String encodedVideogame = URLEncoder.encode(videogame, StandardCharsets.UTF_8);
            String relativeUri = MATCHES_ENDPOINT + "?videogame=" + encodedVideogame + "&page=" + page + "&per_page=" + perPage;

            // Asegurar que enviamos también el token como query param (algunas llamadas funcionan con token param)
            String tokenParam = "&token=" + URLEncoder.encode(apiKey, StandardCharsets.UTF_8);
            if (!relativeUri.contains("token=")) {
                relativeUri = relativeUri + tokenParam;
            }

            String finalUrl = baseUrl + relativeUri;

            // Logueo informativo y debug de apiKey enmascarada
            log.info("Fetching PandaScore matches from: {} for videogame={}", finalUrl, videogame);
            log.debug("PandaScore API key (masked): {}", apiKey == null ? "(null)" : (apiKey.length() > 4 ? "****" + apiKey.substring(apiKey.length() - 4) : "****"));

            int maxRetries = externalApiProperties.getPandascoreMaxRetries();
            long baseBackoff = externalApiProperties.getPandascoreBaseBackoffMillis();

            int attempt = 0;
            while (true) {
                attempt++;
                try {
                    PandaScoreMatchDto[] body = restClientBuilder
                        .baseUrl(baseUrl)
                        .build()
                        .get()
                        .uri(relativeUri)
                        .header("Authorization", "Bearer " + apiKey)
                        .retrieve()
                        .body(PandaScoreMatchDto[].class);

                    if (body == null) {
                        log.warn("PandaScore API returned null for videogame={}", videogame);
                        return Collections.emptyList();
                    }

                    if (body.length == 0) {
                        log.warn("PandaScore API returned empty result array for videogame={} url={} (attempt={})", videogame, finalUrl, attempt);
                    } else {
                        log.info("Successfully fetched {} matches from PandaScore for videogame={} (attempt={})", body.length, videogame, attempt);
                    }

                    return Arrays.asList(body);
                } catch (RestClientException ex) {
                    String msg = ex.getMessage() == null ? "" : ex.getMessage().toLowerCase();
                    boolean is429 = msg.contains("429") || msg.contains("too many requests") || msg.contains("rate limit");

                    if (is429 && attempt <= maxRetries) {
                        long waitMillis = baseBackoff * (1L << (attempt - 1));
                        log.warn("Received 429 from PandaScore, attempt={} of {}, backing off {}ms", attempt, maxRetries, waitMillis);
                        try {
                            Thread.sleep(waitMillis);
                        } catch (InterruptedException ie) {
                            Thread.currentThread().interrupt();
                            throw new RestClientException("Interrupted during backoff", ie);
                        }
                        continue; // retry
                    }

                    if (attempt <= 1) {
                        long waitMillis = Math.max(200L, baseBackoff);
                        log.warn("Transient error fetching PandaScore (attempt={}): {}, retrying after {}ms", attempt, ex.getMessage(), waitMillis);
                        try {
                            Thread.sleep(waitMillis);
                        } catch (InterruptedException ie) {
                            Thread.currentThread().interrupt();
                            throw new RestClientException("Interrupted during backoff", ie);
                        }
                        continue;
                    }

                    log.error("Error fetching matches from PandaScore for videogame={} url={} attempt={}", videogame, finalUrl, attempt, ex);
                    return Collections.emptyList();
                }
            }
        } catch (Exception e) {
            log.error("Error fetching matches from PandaScore for videogame={}", videogame, e);
            return Collections.emptyList();
        }
    }
}
