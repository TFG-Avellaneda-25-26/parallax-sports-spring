package dev.parallaxsports.bot.controller;

import dev.parallaxsports.bot.dto.BotPermissionResponse;
import dev.parallaxsports.bot.service.BotPermissionCacheService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/bot")
@RequiredArgsConstructor
public class BotCommandController {

    private final BotPermissionCacheService botPermissionCacheService;

    @GetMapping("/check-permission")
    public ResponseEntity<BotPermissionResponse> checkPermission(
        @RequestParam String provider,
        @RequestParam String providerSubject
    ) {
        boolean allowed = botPermissionCacheService.canExecuteCommand(provider, providerSubject);
        return ResponseEntity.ok(new BotPermissionResponse(allowed));
    }
}
