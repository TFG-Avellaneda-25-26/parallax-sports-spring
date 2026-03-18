package dev.parallaxsports.formula1.controller;

import dev.parallaxsports.formula1.dto.Formula1SessionResponse;
import dev.parallaxsports.external.formula1.service.Formula1SyncService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/formula1")
@RequiredArgsConstructor
public class Formula1Controller {

    private final Formula1SyncService formula1SyncService;

    @GetMapping("/sessions/{year}")
    public List<Formula1SessionResponse> sessions(@PathVariable int year) {
        return formula1SyncService.getSessionsForYear(year);
    }
}
