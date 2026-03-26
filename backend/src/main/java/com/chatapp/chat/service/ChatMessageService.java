package com.chatapp.chat.service;

import com.chatapp.chat.dto.ChatMessageDto;
import com.chatapp.chat.dto.ReadReceiptDto;
import com.chatapp.chat.dto.SendMessageRequest;
import com.chatapp.chat.model.ChatMessage;
import com.chatapp.chat.model.MessageStatus;
import com.chatapp.chat.model.MessageType;
import com.chatapp.chat.repository.ChatMessageRepository;
import com.chatapp.common.exception.BadRequestException;
import com.chatapp.common.exception.ResourceNotFoundException;
import com.chatapp.common.exception.UnauthorizedException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ChatMessageService {

    private final ChatMessageRepository chatMessageRepository;
    private final ChatRoomService chatRoomService;
    private final ChatCacheService chatCacheService;

    public ChatMessageDto sendMessage(SendMessageRequest request, Long senderId, String senderUsername) {
        if (!chatRoomService.isUserMemberOfRoom(request.getChatRoomId(), senderId)) {
            throw new BadRequestException("You are not a member of this chat room");
        }

        MessageType messageType = request.getType() != null
                ? MessageType.valueOf(request.getType().toUpperCase())
                : MessageType.TEXT;

        ChatMessage message = ChatMessage.builder()
                .chatRoomId(request.getChatRoomId())
                .senderId(senderId)
                .senderUsername(senderUsername)
                .content(request.getContent())
                .type(messageType)
                .status(MessageStatus.SENT)
                .fileUrl(request.getFileUrl())
                .fileName(request.getFileName())
                .build();

        ChatMessage saved = chatMessageRepository.save(message);

        // Update room's last message
        String preview = messageType == MessageType.TEXT
                ? request.getContent()
                : "[" + messageType.name() + "]";
        chatRoomService.updateLastMessage(request.getChatRoomId(), preview);

        log.debug("Message sent in room {} by user {}", request.getChatRoomId(), senderId);
        return toDto(saved);
    }

    public Page<ChatMessageDto> getChatHistory(String chatRoomId, Long userId, int page, int size) {
        if (!chatRoomService.isUserMemberOfRoom(chatRoomId, userId)) {
            throw new BadRequestException("You are not a member of this chat room");
        }

        // For page 0 with default size, try Redis cache first
        if (page == 0) {
            List<ChatMessageDto> cached = chatCacheService.getRecentMessages(chatRoomId);
            if (!cached.isEmpty()) {
                log.debug("Serving {} cached messages for room {}", cached.size(), chatRoomId);
            }
        }

        Pageable pageable = PageRequest.of(page, size);
        return chatMessageRepository
                .findByChatRoomIdOrderByTimestampDesc(chatRoomId, pageable)
                .map(this::toDto);
    }

    public void markMessagesAsRead(ReadReceiptDto receipt) {
        List<ChatMessage> messages = chatMessageRepository
                .findByChatRoomIdAndStatusAndSenderIdNot(
                        receipt.getChatRoomId(), MessageStatus.SENT, receipt.getReadByUserId());

        messages.forEach(msg -> msg.setStatus(MessageStatus.READ));
        chatMessageRepository.saveAll(messages);
        log.debug("Marked {} messages as read in room {}",
                messages.size(), receipt.getChatRoomId());
    }

    public void updateMessageStatus(String messageId, MessageStatus status) {
        chatMessageRepository.findById(messageId).ifPresent(msg -> {
            msg.setStatus(status);
            chatMessageRepository.save(msg);
        });
    }

    // --- Edit Message ---

    public ChatMessageDto editMessage(String messageId, String newContent, Long userId) {
        ChatMessage message = chatMessageRepository.findById(messageId)
                .orElseThrow(() -> new ResourceNotFoundException("Message not found"));

        if (!message.getSenderId().equals(userId)) {
            throw new UnauthorizedException("You can only edit your own messages");
        }
        if (message.isDeleted()) {
            throw new BadRequestException("Cannot edit a deleted message");
        }

        message.setContent(newContent);
        message.setEditedAt(Instant.now());
        ChatMessage saved = chatMessageRepository.save(message);
        log.debug("Message {} edited by user {}", messageId, userId);
        return toDto(saved);
    }

    // --- Delete Message ---

    public ChatMessageDto deleteMessage(String messageId, Long userId) {
        ChatMessage message = chatMessageRepository.findById(messageId)
                .orElseThrow(() -> new ResourceNotFoundException("Message not found"));

        if (!message.getSenderId().equals(userId)) {
            throw new UnauthorizedException("You can only delete your own messages");
        }

        message.setDeleted(true);
        message.setContent("This message was deleted");
        ChatMessage saved = chatMessageRepository.save(message);
        log.debug("Message {} deleted by user {}", messageId, userId);
        return toDto(saved);
    }

    // --- Reactions ---

    public ChatMessageDto toggleReaction(String messageId, String emoji, Long userId) {
        ChatMessage message = chatMessageRepository.findById(messageId)
                .orElseThrow(() -> new ResourceNotFoundException("Message not found"));

        if (message.isDeleted()) {
            throw new BadRequestException("Cannot react to a deleted message");
        }

        // Toggle: add if not present, remove if already reacted
        var users = message.getReactions().get(emoji);
        if (users != null && users.contains(userId)) {
            message.removeReaction(emoji, userId);
        } else {
            message.addReaction(emoji, userId);
        }

        ChatMessage saved = chatMessageRepository.save(message);
        log.debug("Reaction {} toggled on message {} by user {}", emoji, messageId, userId);
        return toDto(saved);
    }

    // --- Search Messages ---

    public Page<ChatMessageDto> searchMessages(String chatRoomId, String query, Long userId, int page, int size) {
        if (!chatRoomService.isUserMemberOfRoom(chatRoomId, userId)) {
            throw new BadRequestException("You are not a member of this chat room");
        }

        // Escape regex special characters to prevent injection
        String escapedQuery = Pattern.quote(query);
        Pageable pageable = PageRequest.of(page, size);
        return chatMessageRepository.searchMessages(chatRoomId, escapedQuery, pageable)
                .map(this::toDto);
    }

    // --- Export Chat History ---

    public String exportChatHistory(String chatRoomId, Long userId) {
        if (!chatRoomService.isUserMemberOfRoom(chatRoomId, userId)) {
            throw new BadRequestException("You are not a member of this chat room");
        }

        List<ChatMessage> messages = chatMessageRepository
                .findByChatRoomIdAndDeletedFalseOrderByTimestampAsc(chatRoomId);

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
                .withZone(ZoneOffset.UTC);

        return messages.stream()
                .map(msg -> String.format("[%s] %s: %s",
                        formatter.format(msg.getTimestamp()),
                        msg.getSenderUsername(),
                        msg.getContent()))
                .collect(Collectors.joining("\n"));
    }

    private ChatMessageDto toDto(ChatMessage message) {
        return ChatMessageDto.builder()
                .id(message.getId())
                .chatRoomId(message.getChatRoomId())
                .senderId(message.getSenderId())
                .senderUsername(message.getSenderUsername())
                .content(message.getContent())
                .type(message.getType())
                .status(message.getStatus())
                .fileUrl(message.getFileUrl())
                .fileName(message.getFileName())
                .timestamp(message.getTimestamp())
                .editedAt(message.getEditedAt())
                .deleted(message.isDeleted())
                .reactions(message.getReactions())
                .build();
    }
}
