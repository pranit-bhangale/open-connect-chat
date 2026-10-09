package com.pranit.connect.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record TypingRequest(
        @NotBlank(message = "Sender ID is required")
        @Size(min = 1, max = 100, message = "Sender ID must not exceed 100 characters")
        String senderId,

        @NotBlank(message = "Sender name is required")
        @Size(min = 1, max = 100, message = "Sender name must not exceed 100 characters")
        String senderName,

        boolean isTyping
) {
}
