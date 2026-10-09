package com.pranit.connect.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record OpenChatRequest(
        @NotBlank(message = "Sender ID is required")
        @Size(min = 1, max = 50, message = "Sender ID must be between 1 and 50 characters")
        String senderId,

        @NotBlank(message = "Sender name is required")
        @Size(min = 2, max = 50, message = "Sender name must be between 2 and 50 characters")
        String senderName
) {
}
