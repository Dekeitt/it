package com.clean.it.util;

import org.springframework.data.redis.connection.ReturnType;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Objects;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Simple distributed lock using Redis SETNX + Lua for safe release.
 * Not a full replacement for Redisson but sufficient for critical sections.
 */
@Component
public class RedisLockService {

    private final RedisTemplate<String, Object> redisTemplate;
    private static final Logger log = LoggerFactory.getLogger(RedisLockService.class);

    public RedisLockService(RedisTemplate<String, Object> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    /**
     * Try to acquire a lock and return a token if successful. Token must be provided to release the lock.
     */
    public String tryLock(String key, Duration ttl) {
        String token = UUID.randomUUID().toString();
        try {
            Boolean success = redisTemplate.opsForValue().setIfAbsent(key, token, ttl);
            if (Boolean.TRUE.equals(success)) return token;
            return null;
        } catch (Exception e) {
            log.error("Failed to acquire redis lock {}: {}", key, e.getMessage(), e);
            return null;
        }
    }

    /**
     * Release lock only if token matches. Uses Lua script for atomicity.
     */
    public boolean releaseLock(String key, String token) {
        if (token == null) return false;
        // Lua: if redis.call('get',KEYS[1]) == ARGV[1] then return redis.call('del',KEYS[1]) else return 0 end
        byte[] script = "if redis.call('get',KEYS[1]) == ARGV[1] then return redis.call('del',KEYS[1]) else return 0 end".getBytes(StandardCharsets.UTF_8);
        try {
            RedisCallback<Long> callback = connection -> connection.eval(script, ReturnType.INTEGER, 1,
                    key.getBytes(StandardCharsets.UTF_8), token.getBytes(StandardCharsets.UTF_8));
            Long result = redisTemplate.execute(callback);
            return Objects.equals(1L, result);
        } catch (Exception e) {
            log.error("Failed to release redis lock {}: {}", key, e.getMessage(), e);
            return false;
        }
    }
}

