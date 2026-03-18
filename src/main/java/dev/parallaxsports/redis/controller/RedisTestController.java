package dev.parallaxsports.redis.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/redis")
public class RedisTestController {

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @GetMapping("/save")
    public String save() {
        redisTemplate.opsForValue().set("test:key", "Hola Redis");
        return "Guardado!";
    }

    @GetMapping("/get")
    public Object get() {
        return redisTemplate.opsForValue().get("test:key");
    }
}
