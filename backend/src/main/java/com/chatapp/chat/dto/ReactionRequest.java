package com.chatapp.chat.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ReactionRequest {

    private String messageId;

    @NotBlank(message = "Emoji is required")
    private String emoji;
}
