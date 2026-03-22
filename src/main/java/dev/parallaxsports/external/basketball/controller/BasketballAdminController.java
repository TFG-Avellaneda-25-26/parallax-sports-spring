package dev.parallaxsports.external.basketball.controller;

import dev.parallaxsports.basketball.BasketballLeague;
import dev.parallaxsports.external.basketball.dto.BasketballSyncResponse;
import dev.parallaxsports.external.basketball.service.BasketballSyncService;
import java.time.LocalDate;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/basketball")
@RequiredArgsConstructor
public class BasketballAdminController {

    private final BasketballSyncService basketballSyncService;

    @PostMapping("/sync")
    public ResponseEntity<BasketballSyncResponse> sync(
        @RequestParam BasketballLeague league,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate,
        @RequestParam(required = false) Integer maxPages
    ) {
        return ResponseEntity.ok(basketballSyncService.syncRange(league, fromDate, toDate, maxPages));
    }
}
