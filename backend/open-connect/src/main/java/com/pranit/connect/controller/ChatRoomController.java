package com.pranit.connect.controller;

import com.pranit.connect.dto.ChatRoomResponse;
import com.pranit.connect.dto.CloseChatRoomRequest;
import com.pranit.connect.dto.CreateChatRoomRequest;
import com.pranit.connect.dto.JoinChatRoomRequest;
import com.pranit.connect.dto.LeaveChatRequest;
import com.pranit.connect.dto.MessageResponse;
import com.pranit.connect.kafka.KafkaChatPublisher;
import com.pranit.connect.service.ChatRoomService;
import com.pranit.connect.wrapper.PageResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/rooms")
@RequiredArgsConstructor
@Validated
public class ChatRoomController {

    private final ChatRoomService chatRoomService;
    private final SimpMessagingTemplate messagingTemplate;
    private final KafkaChatPublisher kafkaChatPublisher;

    @PostMapping(version = "v1")
    public ResponseEntity<ChatRoomResponse> createChatRoom(@RequestBody @Valid CreateChatRoomRequest request) {
        return ResponseEntity.ok(this.chatRoomService.createChatRoom(request));
    }

    @PostMapping(value = "/join", version = "v1")
    public ResponseEntity<ChatRoomResponse> joinChatRoom(@RequestBody @Valid JoinChatRoomRequest request) {
        return ResponseEntity.ok(this.chatRoomService.joinChatRoom(request));
    }

    @GetMapping(value = "/{roomId}", version = "v1")
    public ResponseEntity<ChatRoomResponse> getChatRoom(
            @PathVariable
            @NotBlank(message = "Room ID is required")
            @Pattern(regexp = "^[0-9a-zA-Z_-]{4,36}$", message = "Room ID must be 4 to 36 alphanumeric characters")
            String roomId
    ) {
        return ResponseEntity.ok(this.chatRoomService.getChatRoom(roomId));
    }

    //@GetMapping(version = "v1")
    public ResponseEntity<PageResponse<ChatRoomResponse>> getRooms(
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "10") @Min(1) @Max(50) int size,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String userId,
            @RequestParam(required = false) String userName
    ) {
        return ResponseEntity.ok(this.chatRoomService.getRooms(page, size, keyword, userId, userName));
    }

    @GetMapping(value = "/{roomId}/messages", version = "v1")
    public ResponseEntity<PageResponse<MessageResponse>> getMessages(
            @PathVariable
            @NotBlank(message = "Room ID is required")
            @Pattern(regexp = "^[0-9a-zA-Z_-]{4,36}$", message = "Room ID must be 4 to 36 alphanumeric characters")
            String roomId,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size,
            @RequestParam(required = false) String keyword
    ) {
        return ResponseEntity.ok(this.chatRoomService.getMessages(roomId, page, size, keyword));
    }

    @PostMapping(value = "/{roomId}/close", version = "v1")
    public ResponseEntity<MessageResponse> closeChatRoom(
            @PathVariable
            @NotBlank(message = "Room ID is required")
            @Pattern(regexp = "^[0-9a-zA-Z_-]{4,36}$", message = "Room ID must be 4 to 36 alphanumeric characters")
            String roomId,
            @RequestBody @Valid CloseChatRoomRequest request
    ) {
        MessageResponse response = this.chatRoomService.closeChatRoom(roomId, request);
        if (response != null) {
            messagingTemplate.convertAndSend("/topic/room/" + roomId, response);
            if (kafkaChatPublisher != null) {
                kafkaChatPublisher.publish(roomId, response);
            }
        }
        return ResponseEntity.ok(response);
    }

    @PostMapping(value = "/{roomId}/leave", version = "v1")
    public ResponseEntity<MessageResponse> leaveChatRoom(
            @PathVariable
            @NotBlank(message = "Room ID is required")
            @Pattern(regexp = "^[0-9a-zA-Z_-]{4,36}$", message = "Room ID must be 4 to 36 alphanumeric characters")
            String roomId,
            @RequestBody @Valid LeaveChatRequest request
    ) {
        MessageResponse response = this.chatRoomService.leaveChat(roomId, request);
        if (response != null) {
            messagingTemplate.convertAndSend("/topic/room/" + roomId, response);
            if (kafkaChatPublisher != null) {
                kafkaChatPublisher.publish(roomId, response);
            }
        }
        return ResponseEntity.ok(response);
    }
}
