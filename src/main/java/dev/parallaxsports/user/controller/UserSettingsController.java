package dev.parallaxsports.user.controller;

import dev.parallaxsports.user.dto.UpdateUserSettingsRequest;
import dev.parallaxsports.user.dto.UserSettingsResponse;
import dev.parallaxsports.user.service.UserSettingsService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users/{userId}/settings")
@RequiredArgsConstructor
public class UserSettingsController {

    private final UserSettingsService userSettingsService;

    @GetMapping
    public UserSettingsResponse get(@PathVariable Long userId) {
        return userSettingsService.findByUserId(userId);
    }

    @PutMapping
    public ResponseEntity<UserSettingsResponse> upsert(
        @PathVariable Long userId,
        @Valid @RequestBody UpdateUserSettingsRequest request
    ) {
        return ResponseEntity.ok(userSettingsService.upsert(userId, request));
    }
}
