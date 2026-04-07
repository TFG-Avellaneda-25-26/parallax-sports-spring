package dev.parallaxsports.bot.controller;

import dev.parallaxsports.bot.dto.BotPermissionResponse;
import dev.parallaxsports.bot.service.BotPermissionCacheService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Endpoint called by Discord/Telegram bots to pre-validate whether a provider
 * user is allowed to execute commands.
 *
 * <p>Flow:
 * <ol>
 *   <li>Bot receives a command from a user identified by {@code providerSubject}.</li>
 *   <li>Bot calls {@code GET /api/bot/check-permission?provider=discord&providerSubject=...}.</li>
 *   <li>If {@code canExecuteCommand} is true, proceed; otherwise reject the command.</li>
 * </ol>
 *
 * <p>Results are cached in Redis (1-day TTL). The cache is evicted automatically when
 * the user unlinks their provider or deletes their account.
 *
 * <p>TODO: protect with a shared API key (X-Bot-Api-Key header) before exposing publicly.
 */
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
