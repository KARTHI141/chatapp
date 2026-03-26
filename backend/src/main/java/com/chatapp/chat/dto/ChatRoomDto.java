package com.chatapp.chat.dto;

import com.chatapp.chat.model.ChatRoomType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.Set;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatRoomDto {

    private String id;
    private String name;
    private ChatRoomType type;
    private Set<Long> memberIds;
    private Long createdBy;
    private Instant createdAt;
    private String lastMessage;
    private Instant lastMessageAt;
    private long unreadCount;
}
