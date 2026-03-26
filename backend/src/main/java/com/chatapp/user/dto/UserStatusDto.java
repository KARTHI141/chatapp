package com.chatapp.user.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserStatusDto {

    private Long userId;
    private String username;
    private boolean online;
    private Instant lastSeen;
}
