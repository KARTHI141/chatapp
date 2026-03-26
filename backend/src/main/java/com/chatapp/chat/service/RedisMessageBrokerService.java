package com.chatapp.chat.service;

import com.chatapp.chat.dto.ChatMessageDto;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;

/**
 * Redis Pub/Sub service for scaling WebSocket messaging across multiple instances.
 * When a message is published by one instance, all other instances receive it
 * and forward to their local WebSocket clients.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class RedisMessageBrokerService implements MessageListener {

    private static final String CHAT_CHANNEL = "chat:messages";

    private final RedisTemplate<String, Object> redisTemplate;
    private final RedisMessageListenerContainer listenerContainer;
    private final SimpMessagingTemplate messagingTemplate;
    private final ObjectMapper objectMapper;

    @PostConstruct
    public void init() {
        listenerContainer.addMessageListener(this, new ChannelTopic(CHAT_CHANNEL));
        log.info("Redis Pub/Sub listener registered for channel: {}", CHAT_CHANNEL);
    }

    /**
     * Publish a message to the Redis channel for cross-instance delivery.
     */
    public void publishMessage(ChatMessageDto message) {
        redisTemplate.convertAndSend(CHAT_CHANNEL, message);
        log.debug("Published message to Redis channel: {}", CHAT_CHANNEL);
    }

    /**
     * Receive messages from Redis channel and forward to local WebSocket clients.
     */
    @Override
    public void onMessage(Message message, byte[] pattern) {
        try {
            ChatMessageDto chatMessage = objectMapper.readValue(
                    message.getBody(), ChatMessageDto.class);

            messagingTemplate.convertAndSend(
                    "/topic/room." + chatMessage.getChatRoomId(),
                    chatMessage);

            log.debug("Forwarded Redis message to WebSocket topic for room: {}",
                    chatMessage.getChatRoomId());
        } catch (Exception e) {
            log.error("Error processing Redis message: {}", e.getMessage());
        }
    }
}
