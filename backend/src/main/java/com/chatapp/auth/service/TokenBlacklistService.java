package com.chatapp.auth.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

/**
 * Blacklists JWT tokens in Redis on logout.
 * Tokens are stored with a TTL matching their remaining validity period,
 * so they are automatically purged from Redis when they would have expired naturally.
 *
 * Interview note: This is a common pattern for stateless JWT logout.
 * Since JWTs are self-contained and cannot be "invalidated" server-side,
 * a blacklist (backed by Redis for speed) stores revoked tokens until expiry.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class TokenBlacklistService {

    private static final String BLACKLIST_PREFIX = "jwt:blacklist:";

    private final RedisTemplate<String, Object> redisTemplate;

    /**
     * Blacklist a token for the given duration (should be the remaining TTL of the token).
     */
    public void blacklist(String token, Duration ttl) {
        String key = BLACKLIST_PREFIX + token;
        redisTemplate.opsForValue().set(key, "revoked", ttl);
        log.debug("Token blacklisted, TTL: {}s", ttl.getSeconds());
    }

    /**
     * Check if a token has been blacklisted (i.e., user has logged out).
     */
    public boolean isBlacklisted(String token) {
        String key = BLACKLIST_PREFIX + token;
        return Boolean.TRUE.equals(redisTemplate.hasKey(key));
    }
}
