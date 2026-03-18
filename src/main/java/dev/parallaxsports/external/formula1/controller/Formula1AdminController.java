package dev.parallaxsports.external.formula1.controller;

import dev.parallaxsports.formula1.dto.Formula1SyncResponse;
import dev.parallaxsports.external.formula1.service.Formula1SyncService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/formula1")
@RequiredArgsConstructor
public class Formula1AdminController {

    private final Formula1SyncService formula1SyncService;

    @PostMapping("/sync/{year}")
    public ResponseEntity<Formula1SyncResponse> syncYear(@PathVariable int year) {
        return ResponseEntity.ok(formula1SyncService.syncYear(year));
    }
}