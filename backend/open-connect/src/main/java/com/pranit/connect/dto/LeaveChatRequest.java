package com.pranit.connect.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record LeaveChatRequest(
        @NotBlank(message = "Sender ID is required")
        @Size(min = 1, max = 50, message = "Sender ID must not exceed 50 characters")
        String senderId,

        @NotBlank(message = "Sender name is required")
        @Size(min = 1, max = 50, message = "Sender name must not exceed 50 characters")
        String senderName
) {
}
