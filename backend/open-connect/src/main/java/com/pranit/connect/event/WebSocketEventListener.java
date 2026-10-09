package com.pranit.connect.event;

import com.pranit.connect.dto.LeaveChatRequest;
import com.pranit.connect.dto.MessageResponse;
import com.pranit.connect.kafka.KafkaChatPublisher;
import com.pranit.connect.service.ChatRoomService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

import java.util.Map;

/**
 * Listener for WebSocket lifecycle events.
 * Handles automatic cleanup when a client is disconnected, times out, or becomes unresponsive,
 * ensuring no ghost sessions or blocked connections remain.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class WebSocketEventListener {

    private final ChatRoomService chatRoomService;
    private final SimpMessagingTemplate messagingTemplate;
    private final KafkaChatPublisher kafkaChatPublisher;

    @EventListener
    public void handleWebSocketDisconnectListener(final SessionDisconnectEvent event) {
        StompHeaderAccessor headerAccessor = StompHeaderAccessor.wrap(event.getMessage());
        Map<String, Object> sessionAttributes = headerAccessor.getSessionAttributes();
        if (sessionAttributes != null) {
            final String roomId = (String) sessionAttributes.get("roomId");
            final String userId = (String) sessionAttributes.get("userId");
            final String userName = (String) sessionAttributes.get("userName");
            if (roomId != null && userName != null) {
                log.info("Client session {} disconnected (user '{}' from room '{}'). Cleaning up connection.",
                        headerAccessor.getSessionId(), userName, roomId);
                LeaveChatRequest leaveRequest = new LeaveChatRequest(userId, userName);
                MessageResponse leaveResponse = this.chatRoomService.leaveChat(roomId, leaveRequest);
                if (leaveResponse != null) {
                    this.messagingTemplate.convertAndSend("/topic/room/" + roomId, leaveResponse);
                    if (this.kafkaChatPublisher != null) {
                        this.kafkaChatPublisher.publish(roomId, leaveResponse);
                    }
                }
            }
        }
    }
}
