package com.pranit.connect.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record JoinChatRoomRequest(
        @NotBlank(message = "Name is required")
        @Size(min = 2, max = 50, message = "Name must be between 2 and 50 characters")
        String name,

        @NotBlank(message = "Room ID is required")
        @Pattern(regexp = "^[0-9a-zA-Z_-]{4,36}$", message = "Room ID must be 4 to 36 alphanumeric characters")
        String roomId,

        String memberId
) {
    public JoinChatRoomRequest(String name, String roomId) {
        this(name, roomId, null);
    }
}
