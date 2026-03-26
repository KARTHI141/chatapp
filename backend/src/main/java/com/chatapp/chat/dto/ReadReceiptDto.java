package com.chatapp.chat.dto;

import com.chatapp.chat.model.MessageStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReadReceiptDto {

    private String chatRoomId;
    private List<String> messageIds;
    private Long readByUserId;
    private MessageStatus status;
}
