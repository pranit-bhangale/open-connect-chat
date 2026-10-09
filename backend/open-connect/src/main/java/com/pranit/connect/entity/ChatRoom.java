package com.pranit.connect.entity;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Document(collection = "chatRooms")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
public class ChatRoom {

    @Id
    private String id;

    @Indexed(unique = true)
    private String roomId;

    private String roomName;

    private String ownerId;

    private String ownerName;

    @Builder.Default
    private boolean isOpen = true;

    @Builder.Default
    private boolean isClosed = false;

    private Instant closedAt;

    private String closedBy;

    @Builder.Default
    private Instant createdAt = Instant.now();

    @Builder.Default
    private List<ChatMember> members = new ArrayList<>();
}
