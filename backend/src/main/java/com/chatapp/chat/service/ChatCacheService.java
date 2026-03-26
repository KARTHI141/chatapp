package com.chatapp.chat.service;

import com.chatapp.chat.dto.ChatMessageDto;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Collections;
import java.util.List;

/**
 * Caches recent chat messages in Redis for fast retrieval,
 * avoiding MongoDB hits for frequently accessed rooms.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ChatCacheService {

    private static final String CACHE_PREFIX = "chat:recent:";
    private static final int MAX_CACHED_MESSAGES = 50;
    private static final Duration CACHE_TTL = Duration.ofHours(1);

    private final RedisTemplate<String, Object> redisTemplate;
    private final ObjectMapper objectMapper;

    public void cacheMessage(ChatMessageDto message) {
        String key = CACHE_PREFIX + message.getChatRoomId();
        redisTemplate.opsForList().leftPush(key, message);
        redisTemplate.opsForList().trim(key, 0, MAX_CACHED_MESSAGES - 1);
        redisTemplate.expire(key, CACHE_TTL);
    }

    public List<ChatMessageDto> getRecentMessages(String chatRoomId) {
        String key = CACHE_PREFIX + chatRoomId;
        List<Object> raw = redisTemplate.opsForList().range(key, 0, MAX_CACHED_MESSAGES - 1);

        if (raw == null || raw.isEmpty()) {
            return Collections.emptyList();
        }

        return raw.stream()
                .map(obj -> objectMapper.convertValue(obj, ChatMessageDto.class))
                .toList();
    }

    public void invalidateCache(String chatRoomId) {
        redisTemplate.delete(CACHE_PREFIX + chatRoomId);
    }
}
