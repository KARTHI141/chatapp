package com.chatapp.chat.model;

import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.HashSet;
import java.util.Set;

@Document(collection = "chat_rooms")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatRoom {

    @Id
    private String id;

    private String name;

    @Builder.Default
    private ChatRoomType type = ChatRoomType.PRIVATE;

    @Indexed
    @Builder.Default
    private Set<Long> memberIds = new HashSet<>();

    private Long createdBy;

    @CreatedDate
    private Instant createdAt;

    private String lastMessage;

    @Indexed
    private Instant lastMessageAt;
}
