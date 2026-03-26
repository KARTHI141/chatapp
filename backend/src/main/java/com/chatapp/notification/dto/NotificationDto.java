package com.chatapp.notification.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationDto {

    public enum NotificationType {
        NEW_MESSAGE,
        GROUP_INVITE,
        USER_ONLINE,
        SYSTEM
    }

    private String id;
    private Long recipientId;
    private NotificationType type;
    private String title;
    private String body;
    private String referenceId; // e.g., chatRoomId or messageId
    private boolean read;
    private Instant createdAt;
}
