package com.chatapp.chat.repository;

import com.chatapp.chat.model.ChatMessage;
import com.chatapp.chat.model.MessageStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ChatMessageRepository extends MongoRepository<ChatMessage, String> {

    Page<ChatMessage> findByChatRoomIdOrderByTimestampDesc(String chatRoomId, Pageable pageable);

    List<ChatMessage> findByChatRoomIdAndStatusAndSenderIdNot(
            String chatRoomId, MessageStatus status, Long senderId);

    long countByChatRoomIdAndStatusAndSenderIdNot(
            String chatRoomId, MessageStatus status, Long senderId);

    @Query("{ 'chatRoomId': ?0, 'content': { $regex: ?1, $options: 'i' }, 'deleted': { $ne: true } }")
    Page<ChatMessage> searchMessages(String chatRoomId, String query, Pageable pageable);

    List<ChatMessage> findByChatRoomIdAndDeletedFalseOrderByTimestampAsc(String chatRoomId);
}
