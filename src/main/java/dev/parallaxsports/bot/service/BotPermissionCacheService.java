package dev.parallaxsports.bot.service;

import dev.parallaxsports.user.repository.UserIdentityRepository;
import java.util.concurrent.TimeUnit;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;


@Service
@RequiredArgsConstructor
@Slf4j
public class BotPermissionCacheService {

    private static final String KEY_PREFIX = "bot:cmd:";
    private static final long TTL_SECONDS = 86_400L; // 1 day

    private final StringRedisTemplate stringRedisTemplate;
    private final UserIdentityRepository userIdentityRepository;


    public boolean canExecuteCommand(String provider, String providerSubject) {
        String key = buildKey(provider, providerSubject);

        try {
            if (Boolean.TRUE.equals(stringRedisTemplate.hasKey(key))) {
                log.debug("Bot permission cache HIT provider='{}' subject='{}'", provider, providerSubject);
                return true;
            }
        } catch (Exception ex) {
            log.warn("Bot permission Redis check failed, falling back to DB: {}", ex.getMessage());
        }

        boolean exists = userIdentityRepository
            .findByProviderAndProviderSubject(provider, providerSubject)
            .isPresent();

        if (exists) {
            try {
                stringRedisTemplate.opsForValue().set(key, "1", TTL_SECONDS, TimeUnit.SECONDS);
                log.debug("Bot permission cached provider='{}' subject='{}'", provider, providerSubject);
            } catch (Exception ex) {
                log.warn("Bot permission cache write failed: {}", ex.getMessage());
            }
        }

        return exists;
    }


    public void evict(String provider, String providerSubject) {
        try {
            stringRedisTemplate.delete(buildKey(provider, providerSubject));
            log.debug("Bot permission cache evicted provider='{}' subject='{}'", provider, providerSubject);
        } catch (Exception ex) {
            log.warn("Bot permission cache eviction failed: {}", ex.getMessage());
        }
    }

    private static String buildKey(String provider, String providerSubject) {
        return KEY_PREFIX + provider + ":" + providerSubject;
    }
}
