package dev.parallaxsports.external.pandascore.controller;

import dev.parallaxsports.external.pandascore.dto.PandaScoreMatchResponse;
import dev.parallaxsports.external.pandascore.service.PandaScoreMatchService;
import java.time.OffsetDateTime;
import java.util.Collections;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
@Slf4j
public class PandaScorePublicController {

    private final PandaScoreMatchService matchService;

    @GetMapping("/league-of-legends/matches")
    public List<PandaScoreMatchResponse> getLeagueOfLegendsMatches(
        @RequestParam(required = false) String leagueName,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime startDate,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime endDate
    ) {
        return getMatchesForVideogame("league-of-legends", leagueName, startDate, endDate);
    }

    @GetMapping("/valorant/matches")
    public List<PandaScoreMatchResponse> getValorantMatches(
        @RequestParam(required = false) String leagueName,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime startDate,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime endDate
    ) {
        return getMatchesForVideogame("valorant", leagueName, startDate, endDate);
    }

    @GetMapping("/dota2/matches")
    public List<PandaScoreMatchResponse> getDota2Matches(
        @RequestParam(required = false) String leagueName,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime startDate,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime endDate
    ) {
        return getMatchesForVideogame("dota2", leagueName, startDate, endDate);
    }

    @GetMapping("/counter-strike/matches")
    public List<PandaScoreMatchResponse> getCounterStrikeMatches(
        @RequestParam(required = false) String leagueName,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime startDate,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime endDate
    ) {
        return getMatchesForVideogame("counter-strike", leagueName, startDate, endDate);
    }

    @GetMapping("/overwatch/matches")
    public List<PandaScoreMatchResponse> getOverwatchMatches(
        @RequestParam(required = false) String leagueName,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime startDate,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime endDate
    ) {
        return getMatchesForVideogame("overwatch", leagueName, startDate, endDate);
    }

    private List<PandaScoreMatchResponse> getMatchesForVideogame(String videogame, String leagueName, OffsetDateTime startDate, OffsetDateTime endDate) {
        try {
            if (leagueName != null && !leagueName.isBlank()) {
                log.info("Public: Getting matches for league={} videogame={}", leagueName, videogame);
                if (startDate != null && endDate != null) {
                    return matchService.getMatchesByLeagueBetweenDates(leagueName, startDate, endDate);
                }
                return matchService.getMatchesByLeague(leagueName);
            }

            if (startDate != null && endDate != null) {
                log.info("Public: Getting matches for videogame={} between {} and {}", videogame, startDate, endDate);
                return matchService.getMatchesByVideogameBetweenDates(videogame, startDate, endDate);
            }

            log.info("Public: Getting matches for videogame={}", videogame);
            return matchService.getMatchesByVideogame(videogame);
        } catch (DataAccessException dae) {
            log.error("Public: DB access error retrieving matches for videogame={}: {}", videogame, dae.getMessage(), dae);
            return Collections.emptyList();
        } catch (Exception e) {
            log.error("Public: Unexpected error retrieving matches for videogame={}", videogame, e);
            return Collections.emptyList();
        }
    }
}


