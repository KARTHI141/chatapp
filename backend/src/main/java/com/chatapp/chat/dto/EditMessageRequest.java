package com.chatapp.chat.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class EditMessageRequest {

    private String messageId;

    @NotBlank(message = "Content is required")
    private String content;
}
