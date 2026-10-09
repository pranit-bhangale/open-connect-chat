package com.pranit.connect.kafka;

import com.pranit.connect.dto.MessageResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import tools.jackson.databind.ObjectMapper;

/**
 * Service responsible for publishing chat events to the distributed Kafka cluster.
 * Keys messages by roomId so that all messages within a given chat room land on
 * the same partition, guaranteeing strict chronological ordering.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class KafkaChatPublisher {

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;
    private final String clusterInstanceId;

    @Value("${chat.kafka.topic:chat-messages}")
    private String chatTopic;

    /**
     * Publishes a chat event to Kafka.
     *
     * @param roomId  the chat room ID (used as Kafka partition key)
     * @param payload the message payload
     */
    public void publish(final String roomId, final MessageResponse payload) {
        if (roomId == null || payload == null) return;
        try {
            final KafkaChatMessageEnvelope envelope = KafkaChatMessageEnvelope.builder()
                    .instanceId(this.clusterInstanceId)
                    .roomId(roomId)
                    .payload(payload)
                    .build();
            final String json = this.objectMapper.writeValueAsString(envelope);
            // Kafka partition key is roomId to guarantee sequential ordering per chat room
            this.kafkaTemplate.send(chatTopic, roomId, json);
            log.debug("Published to Kafka topic {} [key: {}]: type: {}, sender: {}",
                    chatTopic, roomId, payload.type(), payload.senderName());
        } catch (Exception e) {
            log.warn("Failed to publish message to Kafka for room {}: {}", roomId, e.getMessage());
        }

    }
}
