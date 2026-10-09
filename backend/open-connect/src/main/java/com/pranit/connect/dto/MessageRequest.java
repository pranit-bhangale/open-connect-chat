package com.pranit.connect.dto;

import com.pranit.connect.entity.MessageType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record MessageRequest(
        @NotBlank(message = "Sender ID is required")
        @Size(min = 1, max = 50, message = "Sender ID must not exceed 50 characters")
        String senderId,

        @Size(max = 50, message = "Sender name must not exceed 50 characters")
        String senderName,

        @NotBlank(message = "Message content cannot be blank")
        @Size(min = 1, max = 2000, message = "Message content must be between 1 and 2000 characters")
        String content,

        MessageType type
) {
    public MessageRequest {
        if (type == null) type = MessageType.CHAT;
    }
}
