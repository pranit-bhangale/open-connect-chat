package com.pranit.connect.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateChatRoomRequest(
        @NotBlank(message = "Owner name is required")
        @Size(min = 2, max = 50, message = "Owner name must be between 2 and 50 characters")
        String owner,

        @NotBlank(message = "Room name is required")
        @Size(min = 2, max = 100, message = "Room name must be between 2 and 100 characters")
        String roomName,

        Boolean isOpen,
        String ownerId
) {
    public CreateChatRoomRequest {
        if (isOpen == null) isOpen = false;
    }

    public CreateChatRoomRequest(String owner, String roomName, Boolean isOpen) {
        this(owner, roomName, isOpen, null);
    }
}
