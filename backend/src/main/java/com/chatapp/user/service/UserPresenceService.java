package com.chatapp.user.service;

import com.chatapp.auth.repository.UserRepository;
import com.chatapp.user.dto.UserStatusDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserPresenceService {

    private static final String ONLINE_USERS_KEY = "online_users";

    private final RedisTemplate<String, Object> redisTemplate;
    private final UserRepository userRepository;
    private final SimpMessagingTemplate messagingTemplate;

    @Transactional
    public void setUserOnline(Long userId, String username) {
        redisTemplate.opsForSet().add(ONLINE_USERS_KEY, userId.toString());

        userRepository.findById(userId).ifPresent(user -> {
            user.setOnline(true);
            userRepository.save(user);
        });

        broadcastStatus(userId, username, true);
        log.info("User online: {} ({})", username, userId);
    }

    @Transactional
    public void setUserOffline(Long userId, String username) {
        redisTemplate.opsForSet().remove(ONLINE_USERS_KEY, userId.toString());

        userRepository.findById(userId).ifPresent(user -> {
            user.setOnline(false);
            user.setLastSeen(Instant.now());
            userRepository.save(user);
        });

        broadcastStatus(userId, username, false);
        log.info("User offline: {} ({})", username, userId);
    }

    public boolean isUserOnline(Long userId) {
        Boolean isMember = redisTemplate.opsForSet().isMember(ONLINE_USERS_KEY, userId.toString());
        return Boolean.TRUE.equals(isMember);
    }

    public Set<Long> getOnlineUserIds() {
        Set<Object> members = redisTemplate.opsForSet().members(ONLINE_USERS_KEY);
        if (members == null) return Set.of();
        return members.stream()
                .map(m -> Long.parseLong(m.toString()))
                .collect(Collectors.toSet());
    }

    private void broadcastStatus(Long userId, String username, boolean online) {
        UserStatusDto status = UserStatusDto.builder()
                .userId(userId)
                .username(username)
                .online(online)
                .lastSeen(online ? null : Instant.now())
                .build();
        messagingTemplate.convertAndSend("/topic/status", status);
    }
}
