package com.chatapp.chat.service;

import com.chatapp.chat.dto.ChatRoomDto;
import com.chatapp.chat.dto.CreateRoomRequest;
import com.chatapp.chat.model.ChatRoom;
import com.chatapp.chat.model.ChatRoomType;
import com.chatapp.chat.model.MessageStatus;
import com.chatapp.chat.repository.ChatMessageRepository;
import com.chatapp.chat.repository.ChatRoomRepository;
import com.chatapp.common.exception.BadRequestException;
import com.chatapp.common.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ChatRoomService {

    private final ChatRoomRepository chatRoomRepository;
    private final ChatMessageRepository chatMessageRepository;

    public ChatRoomDto createRoom(CreateRoomRequest request, Long currentUserId) {
        ChatRoomType type = request.getType() != null
                ? ChatRoomType.valueOf(request.getType().toUpperCase())
                : ChatRoomType.PRIVATE;

        Set<Long> members = new HashSet<>(request.getMemberIds());
        members.add(currentUserId);

        // For private chat, check if room already exists
        if (type == ChatRoomType.PRIVATE && members.size() == 2) {
            Long otherUserId = members.stream()
                    .filter(id -> !id.equals(currentUserId))
                    .findFirst()
                    .orElseThrow(() -> new BadRequestException("Invalid member list"));

            var existingRoom = chatRoomRepository
                    .findByTypeAndMemberIdsContainingAndMemberIdsContaining(
                            ChatRoomType.PRIVATE, currentUserId, otherUserId);

            if (existingRoom.isPresent()) {
                return toDto(existingRoom.get(), currentUserId);
            }
        }

        ChatRoom room = ChatRoom.builder()
                .name(request.getName())
                .type(type)
                .memberIds(members)
                .createdBy(currentUserId)
                .build();

        ChatRoom savedRoom = chatRoomRepository.save(room);
        log.info("Chat room created: {} by user {}", savedRoom.getId(), currentUserId);
        return toDto(savedRoom, currentUserId);
    }

    public List<ChatRoomDto> getUserRooms(Long userId) {
        return chatRoomRepository
                .findByMemberIdsContainingOrderByLastMessageAtDesc(userId)
                .stream()
                .map(room -> toDto(room, userId))
                .collect(Collectors.toList());
    }

    public ChatRoom getRoomById(String roomId) {
        return chatRoomRepository.findById(roomId)
                .orElseThrow(() -> new ResourceNotFoundException("Chat room not found: " + roomId));
    }

    public ChatRoomDto getRoomDtoById(String roomId, Long userId) {
        ChatRoom room = getRoomById(roomId);
        return toDto(room, userId);
    }

    public void updateLastMessage(String roomId, String message) {
        ChatRoom room = getRoomById(roomId);
        room.setLastMessage(message);
        room.setLastMessageAt(java.time.Instant.now());
        chatRoomRepository.save(room);
    }

    public boolean isUserMemberOfRoom(String roomId, Long userId) {
        ChatRoom room = getRoomById(roomId);
        return room.getMemberIds().contains(userId);
    }

    public ChatRoomDto addMember(String roomId, Long memberId, Long currentUserId) {
        ChatRoom room = getRoomById(roomId);
        if (!room.getCreatedBy().equals(currentUserId)) {
            throw new BadRequestException("Only the room creator can add members");
        }
        if (room.getType() != ChatRoomType.GROUP) {
            throw new BadRequestException("Cannot add members to a private chat");
        }
        room.getMemberIds().add(memberId);
        chatRoomRepository.save(room);
        log.info("User {} added to room {} by {}", memberId, roomId, currentUserId);
        return toDto(room, currentUserId);
    }

    public ChatRoomDto removeMember(String roomId, Long memberId, Long currentUserId) {
        ChatRoom room = getRoomById(roomId);
        if (!room.getCreatedBy().equals(currentUserId) && !memberId.equals(currentUserId)) {
            throw new BadRequestException("Only the room creator or the member can remove membership");
        }
        if (room.getType() != ChatRoomType.GROUP) {
            throw new BadRequestException("Cannot remove members from a private chat");
        }
        room.getMemberIds().remove(memberId);
        chatRoomRepository.save(room);
        log.info("User {} removed from room {} by {}", memberId, roomId, currentUserId);
        return toDto(room, currentUserId);
    }

    public ChatRoomDto renameRoom(String roomId, String newName, Long currentUserId) {
        ChatRoom room = getRoomById(roomId);
        if (!room.getCreatedBy().equals(currentUserId)) {
            throw new BadRequestException("Only the room creator can rename the room");
        }
        if (room.getType() != ChatRoomType.GROUP) {
            throw new BadRequestException("Cannot rename a private chat");
        }
        room.setName(newName);
        chatRoomRepository.save(room);
        log.info("Room {} renamed to '{}' by {}", roomId, newName, currentUserId);
        return toDto(room, currentUserId);
    }

    private ChatRoomDto toDto(ChatRoom room, Long currentUserId) {
        long unreadCount = chatMessageRepository
                .countByChatRoomIdAndStatusAndSenderIdNot(
                        room.getId(), MessageStatus.SENT, currentUserId);

        return ChatRoomDto.builder()
                .id(room.getId())
                .name(room.getName())
                .type(room.getType())
                .memberIds(room.getMemberIds())
                .createdBy(room.getCreatedBy())
                .createdAt(room.getCreatedAt())
                .lastMessage(room.getLastMessage())
                .lastMessageAt(room.getLastMessageAt())
                .unreadCount(unreadCount)
                .build();
    }
}
