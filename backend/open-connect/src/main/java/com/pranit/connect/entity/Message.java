package com.pranit.connect.entity;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

@Document(collection = "messages")
@CompoundIndex(
        name = "room_timestamp_desc_idx",
        def = "{'roomId': 1, 'timeStamp': -1}"
)
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
public class Message {

    @Id
    private String id;

    @Indexed
    private String roomId;

    private String senderId;

    private String senderName;

    private String content;

    @Builder.Default
    private MessageType type = MessageType.CHAT;

    @Builder.Default
    private Instant timeStamp = Instant.now();
}