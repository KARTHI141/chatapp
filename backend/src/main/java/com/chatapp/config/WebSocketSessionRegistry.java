package com.chatapp.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Tracks active WebSocket sessions per user.
 * A user can have multiple sessions (e.g., browser + mobile).
 * This is used to determine if a user is truly offline (all sessions disconnected).
 *
 * Interview note: ConcurrentHashMap + ConcurrentHashMap.newKeySet() ensures
 * thread-safe access without explicit synchronization.
 */
@Component
@Slf4j
public class WebSocketSessionRegistry {

    // userId -> set of session IDs
    private final Map<Long, Set<String>> userSessions = new ConcurrentHashMap<>();

    public void registerSession(Long userId, String sessionId) {
        userSessions.computeIfAbsent(userId, k -> ConcurrentHashMap.newKeySet()).add(sessionId);
        log.debug("Session registered: user={}, session={}, total={}", 
                userId, sessionId, getSessionCount(userId));
    }

    public void removeSession(Long userId, String sessionId) {
        Set<String> sessions = userSessions.get(userId);
        if (sessions != null) {
            sessions.remove(sessionId);
            if (sessions.isEmpty()) {
                userSessions.remove(userId);
            }
        }
        log.debug("Session removed: user={}, session={}, remaining={}",
                userId, sessionId, getSessionCount(userId));
    }

    public boolean hasActiveSessions(Long userId) {
        Set<String> sessions = userSessions.get(userId);
        return sessions != null && !sessions.isEmpty();
    }

    public int getSessionCount(Long userId) {
        Set<String> sessions = userSessions.get(userId);
        return sessions != null ? sessions.size() : 0;
    }

    public Set<String> getSessions(Long userId) {
        return userSessions.getOrDefault(userId, Collections.emptySet());
    }
}
