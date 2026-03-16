package dev.parallaxsports.user.service;

import dev.parallaxsports.core.exception.ResourceNotFoundException;
import dev.parallaxsports.user.dto.UpdateUserSettingsRequest;
import dev.parallaxsports.user.dto.UserSettingsResponse;
import dev.parallaxsports.user.model.UserSettings;
import dev.parallaxsports.user.repository.UserSettingsRepository;
import java.time.OffsetDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserSettingsService {

    private final UserSettingsRepository userSettingsRepository;

    public UserSettingsResponse findByUserId(Long userId) {
        UserSettings settings = userSettingsRepository.findById(userId)
            .orElseThrow(() -> new ResourceNotFoundException("User settings not found: " + userId));
        return toResponse(settings);
    }

    public UserSettingsResponse upsert(Long userId, UpdateUserSettingsRequest request) {
        UserSettings settings = userSettingsRepository.findById(userId).orElseGet(UserSettings::new);
        settings.setUserId(userId);
        settings.setTheme(request.theme() == null ? "system" : request.theme());
        settings.setDefaultView(request.defaultView() == null ? "cards" : request.defaultView());
        settings.setTimezone(request.timezone() == null ? "UTC" : request.timezone());
        settings.setLocale(request.locale() == null ? "en" : request.locale());
        settings.setUpdatedAt(OffsetDateTime.now());
        return toResponse(userSettingsRepository.save(settings));
    }

    private UserSettingsResponse toResponse(UserSettings settings) {
        return new UserSettingsResponse(
            settings.getUserId(),
            settings.getTheme(),
            settings.getDefaultView(),
            settings.getTimezone(),
            settings.getLocale()
        );
    }
}
