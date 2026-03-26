package com.chatapp.chat.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class DeleteMessageRequest {

    @NotBlank(message = "Message ID is required")
    private String messageId;
}
