package com.chatapp.chat.dto;

import com.chatapp.chat.model.MessageStatus;
import com.chatapp.chat.model.MessageType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.Map;
import java.util.Set;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatMessageDto {

    private String id;
    private String chatRoomId;
    private Long senderId;
    private String senderUsername;
    private String content;
    private MessageType type;
    private MessageStatus status;
    private String fileUrl;
    private String fileName;
    private Instant timestamp;
    private Instant editedAt;
    private boolean deleted;
    private Map<String, Set<Long>> reactions;
}
