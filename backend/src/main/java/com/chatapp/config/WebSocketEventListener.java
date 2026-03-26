package com.chatapp.config;

import com.chatapp.auth.entity.User;
import com.chatapp.user.service.UserPresenceService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionConnectedEvent;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

/**
 * Listens for WebSocket lifecycle events to track user presence.
 * Uses WebSocketSessionRegistry to handle multi-device scenarios correctly:
 * a user is only marked offline when ALL sessions are disconnected.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class WebSocketEventListener {

    private final UserPresenceService userPresenceService;
    private final WebSocketSessionRegistry sessionRegistry;

    @EventListener
    public void handleWebSocketConnectListener(SessionConnectedEvent event) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(event.getMessage());
        String sessionId = accessor.getSessionId();

        var auth = (UsernamePasswordAuthenticationToken) event.getUser();
        if (auth != null && auth.getPrincipal() instanceof User user) {
            sessionRegistry.registerSession(user.getId(), sessionId);
            userPresenceService.setUserOnline(user.getId(), user.getUsername());
        }
    }

    @EventListener
    public void handleWebSocketDisconnectListener(SessionDisconnectEvent event) {
        String sessionId = event.getSessionId();

        var auth = (UsernamePasswordAuthenticationToken) event.getUser();
        if (auth != null && auth.getPrincipal() instanceof User user) {
            sessionRegistry.removeSession(user.getId(), sessionId);

            // Only mark offline if the user has no remaining sessions
            if (!sessionRegistry.hasActiveSessions(user.getId())) {
                userPresenceService.setUserOffline(user.getId(), user.getUsername());
            } else {
                log.debug("User {} still has {} active sessions",
                        user.getUsername(), sessionRegistry.getSessionCount(user.getId()));
            }
        }
    }
}
