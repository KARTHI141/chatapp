package com.chatapp.chat.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class SendMessageRequest {

    @NotBlank(message = "Chat room ID is required")
    private String chatRoomId;

    @NotBlank(message = "Content is required")
    private String content;

    private String type; // TEXT, IMAGE, FILE
    private String fileUrl;
    private String fileName;
}
