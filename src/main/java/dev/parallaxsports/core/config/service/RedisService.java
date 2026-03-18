package dev.parallaxsports.core.config.service;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Service
@RequiredArgsConstructor
public class RedisService {

    private final RedisTemplate<String, Object> redisTemplate;

    // Guardar un valor simple
    public void saveValue(String key, Object value) {
        redisTemplate.opsForValue().set(key, value);
    }

    // Guardar un valor con TTL
    public void saveValue(String key, Object value, long seconds) {
        redisTemplate.opsForValue().set(key, value, Duration.ofSeconds(seconds));
    }

    // Obtener un valor
    public Object getValue(String key) {
        return redisTemplate.opsForValue().get(key);
    }

    // Eliminar un valor
    public void deleteValue(String key) {
        redisTemplate.delete(key);
    }
}
