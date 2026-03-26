package com.chatapp.chat.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TypingIndicatorDto {

    private String chatRoomId;
    private Long userId;
    private String username;
    private boolean typing;
}
