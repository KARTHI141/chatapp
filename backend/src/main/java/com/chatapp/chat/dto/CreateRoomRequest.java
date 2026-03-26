package com.chatapp.chat.dto;

import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.Set;

@Data
public class CreateRoomRequest {

    private String name;

    @NotEmpty(message = "At least one member is required")
    private Set<Long> memberIds;

    private String type; // PRIVATE or GROUP
}
