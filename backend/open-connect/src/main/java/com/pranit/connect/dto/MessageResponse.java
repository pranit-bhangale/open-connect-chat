package com.pranit.connect.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.pranit.connect.entity.MessageType;
import lombok.Builder;

import java.time.Instant;

@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public record MessageResponse(
        String messageId,
        String roomId,
        String senderId,
        String senderName,
        String content,
        MessageType type,
        Boolean isTyping,
        Instant timeStamp
) {
}
