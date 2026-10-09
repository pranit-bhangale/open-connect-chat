package com.pranit.connect.controller;

import com.pranit.connect.dto.CloseChatRoomRequest;
import com.pranit.connect.dto.LeaveChatRequest;
import com.pranit.connect.dto.MessageRequest;
import com.pranit.connect.dto.MessageResponse;
import com.pranit.connect.dto.OpenChatRequest;
import com.pranit.connect.dto.TypingRequest;
import com.pranit.connect.entity.MessageType;
import com.pranit.connect.kafka.KafkaChatPublisher;
import com.pranit.connect.service.ChatRoomService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.stereotype.Controller;
import org.springframework.validation.annotation.Validated;

import java.time.Instant;

/**
 * Controller handling real-time WebSocket STOMP messaging for Private Chat.
 * Distributes chat events locally via @SendTo and across cluster nodes via Kafka.
 */
@Slf4j
@Controller
@RequiredArgsConstructor
@Validated
public class ChatController {

    private final ChatRoomService chatRoomService;
    private final KafkaChatPublisher kafkaChatPublisher;

    /**
     * Open/Join chat event.
     * Client publishes to /app/chat/{roomId}/open
     * Broadcasts MessageType.JOIN to /topic/room/{roomId} and publishes to Redis.
     */
    @MessageMapping("/chat/{roomId}/open")
    @SendTo("/topic/room/{roomId}")
    public MessageResponse openChat(
            @DestinationVariable @NotBlank(message = "Room ID is required") String roomId,
            @Payload @Valid OpenChatRequest request,
            SimpMessageHeaderAccessor headerAccessor) {
        log.info("User {} opening/joining room {}", request.senderName(), roomId);

        // Track session attributes for automatic disconnect/cleanup when client times out or disconnects
        if (headerAccessor != null && headerAccessor.getSessionAttributes() != null) {
            headerAccessor.getSessionAttributes().put("roomId", roomId);
            headerAccessor.getSessionAttributes().put("userId", request.senderId());
            headerAccessor.getSessionAttributes().put("userName", request.senderName());
        }

        MessageResponse response = this.chatRoomService.openChat(roomId, request);
        if (response != null && kafkaChatPublisher != null) {
            kafkaChatPublisher.publish(roomId, response);
        }
        return response;
    }

    public MessageResponse openChat(String roomId, OpenChatRequest request) {
        return openChat(roomId, request, null);
    }

    /**
     * Send chat message over WebSocket.
     * Client publishes to /app/chat/{roomId}/sendMessage or legacy /app/sendMessage/{roomId}
     * Broadcasts MessageType.CHAT to /topic/room/{roomId} and publishes to Redis.
     */
    @MessageMapping({"/chat/{roomId}/sendMessage", "/sendMessage/{roomId}"})
    @SendTo("/topic/room/{roomId}")
    public MessageResponse sendMessage(
            @DestinationVariable @NotBlank(message = "Room ID is required") String roomId,
            @Payload @Valid MessageRequest request) {
        MessageResponse response = this.chatRoomService.sendMessage(roomId, request);
        if (response != null && kafkaChatPublisher != null) {
            kafkaChatPublisher.publish(roomId, response);
        }
        return response;
    }

    /**
     * Ephemeral typing indicator.
     * Client publishes to /app/chat/{roomId}/typing
     * Broadcasts MessageType.TYPING to /topic/room/{roomId} without DB persistence for sub-millisecond latency.
     */
    @MessageMapping("/chat/{roomId}/typing")
    @SendTo("/topic/room/{roomId}")
    public MessageResponse sendTypingIndicator(
            @DestinationVariable @NotBlank(message = "Room ID is required") String roomId,
            @Payload @Valid TypingRequest request) {
        MessageResponse response = MessageResponse.builder()
                .roomId(roomId)
                .senderId(request.senderId())
                .senderName(request.senderName())
                .type(MessageType.TYPING)
                .isTyping(request.isTyping())
                .timeStamp(Instant.now())
                .build();
        if (kafkaChatPublisher != null) {
            kafkaChatPublisher.publish(roomId, response);
        }
        return response;
    }

    /**
     * Leave chat event.
     * Client publishes to /app/chat/{roomId}/leave
     * Broadcasts MessageType.LEAVE to /topic/room/{roomId} and publishes to Redis.
     */
    @MessageMapping("/chat/{roomId}/leave")
    @SendTo("/topic/room/{roomId}")
    public MessageResponse leaveChat(
            @DestinationVariable @NotBlank(message = "Room ID is required") String roomId,
            @Payload @Valid LeaveChatRequest request,
            SimpMessageHeaderAccessor headerAccessor) {
        if (headerAccessor != null && headerAccessor.getSessionAttributes() != null) {
            headerAccessor.getSessionAttributes().remove("roomId");
        }
        MessageResponse response = this.chatRoomService.leaveChat(roomId, request);
        if (response != null && kafkaChatPublisher != null) {
            kafkaChatPublisher.publish(roomId, response);
        }
        return response;
    }

    public MessageResponse leaveChat(String roomId, LeaveChatRequest request) {
        return leaveChat(roomId, request, null);
    }

    /**
     * Close chat room event (Owner only).
     * Client publishes to /app/chat/{roomId}/close.
     * Broadcasts MessageType.ROOM_CLOSED to /topic/room/{roomId} and Kafka cluster.
     */
    @MessageMapping("/chat/{roomId}/close")
    @SendTo("/topic/room/{roomId}")
    public MessageResponse closeChat(
            @DestinationVariable @NotBlank(message = "Room ID is required") String roomId,
            @Payload @Valid CloseChatRoomRequest request) {
        log.info("Owner {} closing room {}", request.userName(), roomId);
        MessageResponse response = this.chatRoomService.closeChatRoom(roomId, request);
        if (response != null && kafkaChatPublisher != null) {
            kafkaChatPublisher.publish(roomId, response);
        }
        return response;
    }
}
