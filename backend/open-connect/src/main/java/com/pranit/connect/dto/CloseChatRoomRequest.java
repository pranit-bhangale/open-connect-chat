package com.pranit.connect.dto;

import jakarta.validation.constraints.NotBlank;

public record CloseChatRoomRequest(
        String userId,
        @NotBlank(message = "Owner name is required to close room")
        String userName
) {
}
