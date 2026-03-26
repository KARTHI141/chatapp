package com.chatapp.chat.repository;

import com.chatapp.chat.model.ChatRoom;
import com.chatapp.chat.model.ChatRoomType;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ChatRoomRepository extends MongoRepository<ChatRoom, String> {

    List<ChatRoom> findByMemberIdsContainingOrderByLastMessageAtDesc(Long userId);

    Optional<ChatRoom> findByTypeAndMemberIdsContainingAndMemberIdsContaining(
            ChatRoomType type, Long userId1, Long userId2);
}
