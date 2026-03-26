package com.chatapp.chat.model;

import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.index.TextIndexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

@Document(collection = "chat_messages")
@CompoundIndexes({
    @CompoundIndex(name = "idx_room_timestamp", def = "{'chatRoomId': 1, 'timestamp': -1}")
})
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatMessage {

    @Id
    private String id;

    @Indexed
    private String chatRoomId;

    private Long senderId;
    private String senderUsername;

    @TextIndexed
    private String content;

    @Builder.Default
    private MessageType type = MessageType.TEXT;

    @Builder.Default
    private MessageStatus status = MessageStatus.SENT;

    private String fileUrl;
    private String fileName;

    @CreatedDate
    private Instant timestamp;

    // Edit & Delete support
    private Instant editedAt;

    @Builder.Default
    private boolean deleted = false;

    // Emoji reactions: emoji -> set of userIds
    @Builder.Default
    private Map<String, Set<Long>> reactions = new HashMap<>();

    public void addReaction(String emoji, Long userId) {
        reactions.computeIfAbsent(emoji, k -> new HashSet<>()).add(userId);
    }

    public void removeReaction(String emoji, Long userId) {
        Set<Long> users = reactions.get(emoji);
        if (users != null) {
            users.remove(userId);
            if (users.isEmpty()) {
                reactions.remove(emoji);
            }
        }
    }
}
