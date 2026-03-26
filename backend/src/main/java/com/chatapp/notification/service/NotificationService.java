package com.chatapp.notification.service;

import com.chatapp.notification.dto.NotificationDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationService {

    private final SimpMessagingTemplate messagingTemplate;

    public void sendNotification(Long recipientId, NotificationDto.NotificationType type,
                                  String title, String body, String referenceId) {
        NotificationDto notification = NotificationDto.builder()
                .id(UUID.randomUUID().toString())
                .recipientId(recipientId)
                .type(type)
                .title(title)
                .body(body)
                .referenceId(referenceId)
                .read(false)
                .createdAt(Instant.now())
                .build();

        messagingTemplate.convertAndSendToUser(
                recipientId.toString(),
                "/queue/notifications",
                notification);

        log.debug("Notification sent to user {}: {}", recipientId, type);
    }
}
