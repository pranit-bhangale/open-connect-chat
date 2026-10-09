package com.pranit.connect.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;

import java.time.Instant;

@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ChatRoomResponse(
        String roomId,
        String roomName,
        String ownerId,
        String ownerName,
        String memberId,
        String member,
        int memberCount,
        boolean isOpen,
        boolean isClosed,
        Instant createdAt,
        Instant closedAt
) {
}
