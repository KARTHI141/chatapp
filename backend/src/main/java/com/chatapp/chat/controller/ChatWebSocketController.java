package com.chatapp.chat.controller;

import com.chatapp.auth.entity.User;
import com.chatapp.chat.dto.*;
import com.chatapp.chat.model.ChatRoom;
import com.chatapp.chat.service.ChatCacheService;
import com.chatapp.chat.service.ChatMessageService;
import com.chatapp.chat.service.ChatRoomService;
import com.chatapp.chat.service.RedisMessageBrokerService;
import com.chatapp.notification.dto.NotificationDto;
import com.chatapp.notification.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.stereotype.Controller;

@Controller
@RequiredArgsConstructor
@Slf4j
public class ChatWebSocketController {

    private final SimpMessagingTemplate messagingTemplate;
    private final ChatMessageService chatMessageService;
    private final ChatRoomService chatRoomService;
    private final RedisMessageBrokerService redisMessageBrokerService;
    private final ChatCacheService chatCacheService;
    private final NotificationService notificationService;

    /**
     * Handles incoming chat messages via WebSocket.
     * Client sends to: /app/chat.sendMessage
     * Server broadcasts to: /topic/room.{chatRoomId}
     */
    @MessageMapping("/chat.sendMessage")
    public void sendMessage(@Payload SendMessageRequest request,
                            SimpMessageHeaderAccessor headerAccessor) {
        User user = extractUser(headerAccessor);
        ChatMessageDto messageDto = chatMessageService.sendMessage(
                request, user.getId(), user.getUsername());

        // Cache in Redis for fast recent-message retrieval
        chatCacheService.cacheMessage(messageDto);

        // Publish to Redis for cross-instance delivery
        redisMessageBrokerService.publishMessage(messageDto);

        // Deliver to all members via user queues + send notifications to offline/other-room members
        ChatRoom room = chatRoomService.getRoomById(request.getChatRoomId());
        room.getMemberIds().forEach(memberId -> {
            messagingTemplate.convertAndSendToUser(
                    memberId.toString(),
                    "/queue/messages",
                    messageDto);

            // Send push notification to other members
            if (!memberId.equals(user.getId())) {
                notificationService.sendNotification(
                        memberId,
                        NotificationDto.NotificationType.NEW_MESSAGE,
                        user.getUsername(),
                        messageDto.getContent(),
                        request.getChatRoomId());
            }
        });

        log.debug("WS message sent in room {} by {}", request.getChatRoomId(), user.getUsername());
    }

    /**
     * Handles typing indicator events.
     * Client sends to: /app/chat.typing
     * Server broadcasts to: /topic/room.{chatRoomId}.typing
     */
    @MessageMapping("/chat.typing")
    public void handleTyping(@Payload TypingIndicatorDto typingDto,
                             SimpMessageHeaderAccessor headerAccessor) {
        User user = extractUser(headerAccessor);
        typingDto.setUserId(user.getId());
        typingDto.setUsername(user.getUsername());

        messagingTemplate.convertAndSend(
                "/topic/room." + typingDto.getChatRoomId() + ".typing",
                typingDto);
    }

    /**
     * Handles read receipt events.
     * Client sends to: /app/chat.markRead
     */
    @MessageMapping("/chat.markRead")
    public void markAsRead(@Payload ReadReceiptDto receipt,
                           SimpMessageHeaderAccessor headerAccessor) {
        User user = extractUser(headerAccessor);
        receipt.setReadByUserId(user.getId());
        chatMessageService.markMessagesAsRead(receipt);

        // Notify sender(s) about read status
        messagingTemplate.convertAndSend(
                "/topic/room." + receipt.getChatRoomId() + ".read",
                receipt);
    }

    /**
     * Handles message edit events.
     * Client sends to: /app/chat.editMessage
     */
    @MessageMapping("/chat.editMessage")
    public void editMessage(@Payload EditMessageRequest request,
                            SimpMessageHeaderAccessor headerAccessor) {
        User user = extractUser(headerAccessor);
        ChatMessageDto edited = chatMessageService.editMessage(
                request.getMessageId(), request.getContent(), user.getId());

        broadcastToRoom(edited.getChatRoomId(), "/queue/message.edited", edited);
    }

    /**
     * Handles message delete events.
     * Client sends to: /app/chat.deleteMessage
     */
    @MessageMapping("/chat.deleteMessage")
    public void deleteMessage(@Payload DeleteMessageRequest request,
                              SimpMessageHeaderAccessor headerAccessor) {
        User user = extractUser(headerAccessor);
        ChatMessageDto deleted = chatMessageService.deleteMessage(
                request.getMessageId(), user.getId());

        broadcastToRoom(deleted.getChatRoomId(), "/queue/message.deleted", deleted);
    }

    /**
     * Handles message reaction events.
     * Client sends to: /app/chat.toggleReaction
     */
    @MessageMapping("/chat.toggleReaction")
    public void toggleReaction(@Payload ReactionRequest request,
                               SimpMessageHeaderAccessor headerAccessor) {
        User user = extractUser(headerAccessor);
        ChatMessageDto updated = chatMessageService.toggleReaction(
                request.getMessageId(), request.getEmoji(), user.getId());

        broadcastToRoom(updated.getChatRoomId(), "/queue/message.updated", updated);
    }

    private void broadcastToRoom(String roomId, String destination, ChatMessageDto messageDto) {
        ChatRoom room = chatRoomService.getRoomById(roomId);
        room.getMemberIds().forEach(memberId ->
            messagingTemplate.convertAndSendToUser(
                    memberId.toString(), destination, messageDto)
        );
    }

    private User extractUser(SimpMessageHeaderAccessor headerAccessor) {
        UsernamePasswordAuthenticationToken auth =
                (UsernamePasswordAuthenticationToken) headerAccessor.getUser();
        if (auth == null || !(auth.getPrincipal() instanceof User)) {
            throw new IllegalStateException("User not authenticated via WebSocket");
        }
        return (User) auth.getPrincipal();
    }
}
