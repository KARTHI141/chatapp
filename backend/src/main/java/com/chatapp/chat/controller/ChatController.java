package com.chatapp.chat.controller;

import com.chatapp.auth.entity.User;
import com.chatapp.chat.dto.*;
import com.chatapp.chat.service.ChatMessageService;
import com.chatapp.chat.service.ChatRoomService;
import com.chatapp.common.dto.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.nio.charset.StandardCharsets;
import java.util.List;

@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
public class ChatController {

    private final ChatRoomService chatRoomService;
    private final ChatMessageService chatMessageService;

    // --- Chat Room Endpoints ---

    @PostMapping("/rooms")
    public ResponseEntity<ApiResponse<ChatRoomDto>> createRoom(
            @Valid @RequestBody CreateRoomRequest request,
            @AuthenticationPrincipal User currentUser) {
        ChatRoomDto room = chatRoomService.createRoom(request, currentUser.getId());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(room, "Chat room created"));
    }

    @GetMapping("/rooms")
    public ResponseEntity<ApiResponse<List<ChatRoomDto>>> getUserRooms(
            @AuthenticationPrincipal User currentUser) {
        List<ChatRoomDto> rooms = chatRoomService.getUserRooms(currentUser.getId());
        return ResponseEntity.ok(ApiResponse.success(rooms, "Rooms retrieved"));
    }

    @GetMapping("/rooms/{roomId}")
    public ResponseEntity<ApiResponse<ChatRoomDto>> getRoom(
            @PathVariable String roomId,
            @AuthenticationPrincipal User currentUser) {
        ChatRoomDto room = chatRoomService.getRoomDtoById(roomId, currentUser.getId());
        return ResponseEntity.ok(ApiResponse.success(room, "Room retrieved"));
    }

    // --- Message Endpoints ---

    @GetMapping("/rooms/{roomId}/messages")
    public ResponseEntity<ApiResponse<Page<ChatMessageDto>>> getChatHistory(
            @PathVariable String roomId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size,
            @AuthenticationPrincipal User currentUser) {
        Page<ChatMessageDto> messages = chatMessageService
                .getChatHistory(roomId, currentUser.getId(), page, size);
        return ResponseEntity.ok(ApiResponse.success(messages, "Messages retrieved"));
    }

    // --- Group Management Endpoints ---

    @PostMapping("/rooms/{roomId}/members/{memberId}")
    public ResponseEntity<ApiResponse<ChatRoomDto>> addMember(
            @PathVariable String roomId,
            @PathVariable Long memberId,
            @AuthenticationPrincipal User currentUser) {
        ChatRoomDto room = chatRoomService.addMember(roomId, memberId, currentUser.getId());
        return ResponseEntity.ok(ApiResponse.success(room, "Member added"));
    }

    @DeleteMapping("/rooms/{roomId}/members/{memberId}")
    public ResponseEntity<ApiResponse<ChatRoomDto>> removeMember(
            @PathVariable String roomId,
            @PathVariable Long memberId,
            @AuthenticationPrincipal User currentUser) {
        ChatRoomDto room = chatRoomService.removeMember(roomId, memberId, currentUser.getId());
        return ResponseEntity.ok(ApiResponse.success(room, "Member removed"));
    }

    @PutMapping("/rooms/{roomId}/name")
    public ResponseEntity<ApiResponse<ChatRoomDto>> renameRoom(
            @PathVariable String roomId,
            @RequestParam String name,
            @AuthenticationPrincipal User currentUser) {
        ChatRoomDto room = chatRoomService.renameRoom(roomId, name, currentUser.getId());
        return ResponseEntity.ok(ApiResponse.success(room, "Room renamed"));
    }

    // --- Message Edit & Delete ---

    @PutMapping("/messages/{messageId}")
    public ResponseEntity<ApiResponse<ChatMessageDto>> editMessage(
            @PathVariable String messageId,
            @Valid @RequestBody EditMessageRequest request,
            @AuthenticationPrincipal User currentUser) {
        ChatMessageDto message = chatMessageService.editMessage(
                messageId, request.getContent(), currentUser.getId());
        return ResponseEntity.ok(ApiResponse.success(message, "Message edited"));
    }

    @DeleteMapping("/messages/{messageId}")
    public ResponseEntity<ApiResponse<ChatMessageDto>> deleteMessage(
            @PathVariable String messageId,
            @AuthenticationPrincipal User currentUser) {
        ChatMessageDto message = chatMessageService.deleteMessage(messageId, currentUser.getId());
        return ResponseEntity.ok(ApiResponse.success(message, "Message deleted"));
    }

    // --- Reactions ---

    @PostMapping("/messages/{messageId}/reactions")
    public ResponseEntity<ApiResponse<ChatMessageDto>> toggleReaction(
            @PathVariable String messageId,
            @Valid @RequestBody ReactionRequest request,
            @AuthenticationPrincipal User currentUser) {
        ChatMessageDto message = chatMessageService.toggleReaction(
                messageId, request.getEmoji(), currentUser.getId());
        return ResponseEntity.ok(ApiResponse.success(message, "Reaction toggled"));
    }

    // --- Search Messages ---

    @GetMapping("/rooms/{roomId}/messages/search")
    public ResponseEntity<ApiResponse<Page<ChatMessageDto>>> searchMessages(
            @PathVariable String roomId,
            @RequestParam String q,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @AuthenticationPrincipal User currentUser) {
        Page<ChatMessageDto> results = chatMessageService.searchMessages(
                roomId, q, currentUser.getId(), page, size);
        return ResponseEntity.ok(ApiResponse.success(results, "Search results"));
    }

    // --- Export Chat History ---

    @GetMapping("/rooms/{roomId}/export")
    public ResponseEntity<byte[]> exportChat(
            @PathVariable String roomId,
            @AuthenticationPrincipal User currentUser) {
        String content = chatMessageService.exportChatHistory(roomId, currentUser.getId());
        byte[] bytes = content.getBytes(StandardCharsets.UTF_8);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.TEXT_PLAIN);
        headers.setContentDispositionFormData("attachment", "chat-history-" + roomId + ".txt");

        return new ResponseEntity<>(bytes, headers, HttpStatus.OK);
    }
}
