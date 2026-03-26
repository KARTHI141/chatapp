package com.chatapp.chat.service;

import com.chatapp.chat.dto.ChatMessageDto;
import com.chatapp.chat.dto.SendMessageRequest;
import com.chatapp.chat.model.ChatMessage;
import com.chatapp.chat.model.MessageStatus;
import com.chatapp.chat.model.MessageType;
import com.chatapp.chat.repository.ChatMessageRepository;
import com.chatapp.common.exception.BadRequestException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ChatMessageServiceTest {

    @Mock private ChatMessageRepository chatMessageRepository;
    @Mock private ChatRoomService chatRoomService;
    @Mock private ChatCacheService chatCacheService;

    @InjectMocks
    private ChatMessageService chatMessageService;

    private SendMessageRequest sendRequest;

    @BeforeEach
    void setUp() {
        sendRequest = new SendMessageRequest();
        sendRequest.setChatRoomId("room-123");
        sendRequest.setContent("Hello, World!");
    }

    @Test
    @DisplayName("Send message should persist and return DTO")
    void sendMessage_Success() {
        when(chatRoomService.isUserMemberOfRoom("room-123", 1L)).thenReturn(true);

        ChatMessage saved = ChatMessage.builder()
                .id("msg-1")
                .chatRoomId("room-123")
                .senderId(1L)
                .senderUsername("testuser")
                .content("Hello, World!")
                .type(MessageType.TEXT)
                .status(MessageStatus.SENT)
                .timestamp(Instant.now())
                .build();

        when(chatMessageRepository.save(any(ChatMessage.class))).thenReturn(saved);
        doNothing().when(chatRoomService).updateLastMessage(any(), any());

        ChatMessageDto result = chatMessageService.sendMessage(sendRequest, 1L, "testuser");

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo("msg-1");
        assertThat(result.getContent()).isEqualTo("Hello, World!");
        assertThat(result.getSenderId()).isEqualTo(1L);
        assertThat(result.getStatus()).isEqualTo(MessageStatus.SENT);
        verify(chatMessageRepository).save(any(ChatMessage.class));
    }

    @Test
    @DisplayName("Send message should throw when user is not a room member")
    void sendMessage_NotMember_ThrowsBadRequest() {
        when(chatRoomService.isUserMemberOfRoom("room-123", 1L)).thenReturn(false);

        assertThatThrownBy(() -> chatMessageService.sendMessage(sendRequest, 1L, "testuser"))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("You are not a member of this chat room");

        verify(chatMessageRepository, never()).save(any());
    }
}
