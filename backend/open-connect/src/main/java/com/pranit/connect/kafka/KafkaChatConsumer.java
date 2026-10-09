package com.pranit.connect.kafka;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

/**
 * Consumer that listens to the Kafka chat topic.
 * Uses a unique consumer group per application instance so every cluster node
 * receives broadcasts. Relays messages from other cluster instances to locally
 * connected WebSocket clients.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class KafkaChatConsumer {

    private final ObjectMapper objectMapper;
    private final SimpMessagingTemplate messagingTemplate;
    private final String clusterInstanceId;

    @KafkaListener(
            topics = "${chat.kafka.topic:chat-messages}",
            groupId = "#{clusterInstanceId}"
    )
    public void consume(final String message) {
        try {
            KafkaChatMessageEnvelope envelope = this.objectMapper.readValue(message, KafkaChatMessageEnvelope.class);
            if (envelope == null || envelope.payload() == null || envelope.roomId() == null) return;
            // If the message originated from THIS instance, skip to avoid duplicate delivery
            if (this.clusterInstanceId.equals(envelope.instanceId())) {
                log.trace("Skipping message originating from this instance: {}", envelope.instanceId());
                return;
            }
            log.debug("Relaying Kafka message from instance {} to local WebSocket /topic/room/{}",
                    envelope.instanceId(), envelope.roomId());
            // Relay to locally connected WebSocket clients subscribed to this room
            this.messagingTemplate.convertAndSend("/topic/room/" + envelope.roomId(), envelope.payload());
        } catch (Exception e) {
            log.error("Failed to process incoming Kafka chat message", e);
        }
    }
}
