package dev.parallaxsports.pandascore.controller;

import dev.parallaxsports.core.config.properties.ExternalApiProperties;
import dev.parallaxsports.external.pandascore.service.PandaScoreSyncService;
import dev.parallaxsports.pandascore.dto.PandaScoreMatchResponse;
import dev.parallaxsports.pandascore.dto.PandaScoreSyncResponse;
import dev.parallaxsports.pandascore.service.PandaScoreMatchService;
import dev.parallaxsports.core.exception.BadRequestException;
import java.time.OffsetDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

@RestController
// Cambiada la ruta al espacio de admin para usar SecurityConfig en vez de @PreAuthorize
@RequestMapping("/api/admin/pandascore")
@RequiredArgsConstructor
@Slf4j
public class PandaScoreController {

    private final PandaScoreMatchService matchService;
    private final PandaScoreSyncService syncService;
    private final ExternalApiProperties externalApiProperties;

    private static final Set<String> ALLOWED_VIDEOGAMES = Set.of(
        "league-of-legends",
        "valorant",
        "dota2",
        "counter-strike"
    );

    // Ahora exponemos un endpoint POST por cada videojuego para que no haga falta especificarlo en la llamada
    @PostMapping("/sync/league-of-legends")
    public PandaScoreSyncResponse syncLeagueOfLegends(
        @RequestParam(defaultValue = "2") int pages,
        @RequestParam(required = false) Integer perPage
    ) {
        return syncFor("league-of-legends", pages, perPage);
    }

    @PostMapping("/sync/valorant")
    public PandaScoreSyncResponse syncValorant(
        @RequestParam(defaultValue = "2") int pages,
        @RequestParam(required = false) Integer perPage
    ) {
        return syncFor("valorant", pages, perPage);
    }

    @PostMapping("/sync/dota2")
    public PandaScoreSyncResponse syncDota2(
        @RequestParam(defaultValue = "2") int pages,
        @RequestParam(required = false) Integer perPage
    ) {
        return syncFor("dota2", pages, perPage);
    }

    @PostMapping("/sync/counter-strike")
    public PandaScoreSyncResponse syncCounterStrike(
        @RequestParam(defaultValue = "2") int pages,
        @RequestParam(required = false) Integer perPage
    ) {
        return syncFor("counter-strike", pages, perPage);
    }

    // Lógica común de sincronización extraída a método privado
    private PandaScoreSyncResponse syncFor(String videogame, int pages, Integer perPage) {
        try {
            log.info("Requested PandaScore sync videogame={} pages={} perPage={}", videogame, pages, perPage);

            if (!ALLOWED_VIDEOGAMES.contains(videogame)) {
                throw new BadRequestException("videogame not supported");
            }

            // Check API key presence and fail fast with message claro
            // PandaScore API key is validated by the client; if it's missing the client will log and the sync will no-op.

            int defaultPerPage = externalApiProperties.getPandascoreDefaultPerPage();
            int maxPerPage = externalApiProperties.getPandascoreMaxPerPage();
            int maxPages = externalApiProperties.getPandascoreMaxPages();

            int resolvedPerPage = perPage == null ? defaultPerPage : perPage;
            if (resolvedPerPage > maxPerPage) {
                resolvedPerPage = maxPerPage;
            }

            if (pages > maxPages) {
                throw new BadRequestException("pages value too large");
            }

            PandaScoreSyncService.PandaScoreSyncResponse response = syncService.syncVideogame(videogame, pages, resolvedPerPage);
            return new PandaScoreSyncResponse(
                response.videogame(),
                response.matchesFetched(),
                response.matchesUpserted()
            );
        } catch (BadRequestException e) {
            log.warn("Bad request when syncing PandaScore: {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("Error syncing PandaScore for videogame={}", videogame, e);
            throw e;
        }
    }

    // /test endpoint removed: external raw-fetch testing must be done via internal tools or authorized sync

    @GetMapping("/matches")
    public List<PandaScoreMatchResponse> getMatches(
        @RequestParam(required = false) String leagueName,
        @RequestParam(required = false) String videogame,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime startDate,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime endDate
    ) {
        try {
            if (leagueName != null && !leagueName.isBlank()) {
                log.info("Getting matches for league={}", leagueName);
                return matchService.getMatchesByLeague(leagueName);
            }

            if (videogame != null && !videogame.isBlank()) {
                log.info("Getting matches for videogame={}", videogame);
                return matchService.getMatchesByVideogame(videogame);
            }

            if (startDate != null && endDate != null) {
                log.info("Getting matches between startDate={} endDate={}", startDate, endDate);
                return matchService.getMatchesBetweenDates(startDate, endDate);
            }

            log.info("Getting all PandaScore matches");
            return matchService.getAllMatches();
        } catch (DataAccessException dae) {
            log.error("Database access error while retrieving PandaScore matches: {}", dae.getMessage(), dae);
            // Devolver lista vacía en lugar de 500 para evitar fallos duros en entornos sin BD
            return Collections.emptyList();
        } catch (Exception e) {
            log.error("Unexpected error while retrieving PandaScore matches", e);
            // Devolver lista vacía para no exponer 500 por errores inesperados en entorno local
            return Collections.emptyList();
        }
    }
}
