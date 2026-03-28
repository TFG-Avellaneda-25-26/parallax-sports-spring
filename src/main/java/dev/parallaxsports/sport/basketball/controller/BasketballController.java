package dev.parallaxsports.sport.basketball.controller;

import dev.parallaxsports.sport.basketball.BasketballLeague;
import dev.parallaxsports.sport.basketball.dto.BasketballGameResponse;
import dev.parallaxsports.sport.basketball.dto.BasketballTeamResponse;
import dev.parallaxsports.external.basketball.service.BasketballSyncService;
import java.time.LocalDate;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/basketball")
@RequiredArgsConstructor
public class BasketballController {

    private final BasketballSyncService basketballSyncService;

    @GetMapping("/games")
    public List<BasketballGameResponse> games(
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate,
        @RequestParam(required = false) BasketballLeague league
    ) {
        return basketballSyncService.getGames(league, fromDate, toDate);
    }

    @GetMapping("/teams")
    public List<BasketballTeamResponse> teams(
        @RequestParam(defaultValue = "NBA") BasketballLeague league
    ) {
        return basketballSyncService.getTeams(league);
    }
}
