package com.pranit.connect.kafka;

import com.pranit.connect.dto.MessageResponse;
import lombok.Builder;

/**
 * Envelope containing chat event payload along with cluster routing metadata.
 * instanceId allows nodes to recognize and discard self-published broadcasts.
 */
@Builder
public record KafkaChatMessageEnvelope(
        String instanceId,
        String roomId,
        MessageResponse payload
) {
}
