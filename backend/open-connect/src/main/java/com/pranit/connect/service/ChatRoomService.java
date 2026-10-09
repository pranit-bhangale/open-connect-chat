package com.pranit.connect.service;

import com.pranit.connect.dto.ChatRoomResponse;
import com.pranit.connect.dto.CloseChatRoomRequest;
import com.pranit.connect.dto.CreateChatRoomRequest;
import com.pranit.connect.dto.JoinChatRoomRequest;
import com.pranit.connect.dto.LeaveChatRequest;
import com.pranit.connect.dto.MessageRequest;
import com.pranit.connect.dto.MessageResponse;
import com.pranit.connect.dto.OpenChatRequest;
import com.pranit.connect.wrapper.PageResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

public interface ChatRoomService {

    ChatRoomResponse createChatRoom(@Valid CreateChatRoomRequest chatRoomRequest);

    ChatRoomResponse joinChatRoom(@Valid JoinChatRoomRequest joinChatRoomRequest);

    ChatRoomResponse getChatRoom(@NotBlank String roomId);

    PageResponse<ChatRoomResponse> getOpenRooms(@Min(0) int page, @Min(1) @Max(50) int size, String keyword);

    PageResponse<ChatRoomResponse> getRooms(
            @Min(0) int page, @Min(1) @Max(50) int size, String keyword, String userId, String userName);

    PageResponse<MessageResponse> getMessages(
            @NotBlank String roomId, @Min(0) int page, @Min(1) @Max(100) int size, String keyword);

    MessageResponse sendMessage(@NotBlank String roomId, @Valid MessageRequest request);

    MessageResponse openChat(@NotBlank String roomId, @Valid OpenChatRequest request);

    MessageResponse leaveChat(@NotBlank String roomId, @Valid LeaveChatRequest request);

    MessageResponse closeChatRoom(@NotBlank String roomId, @Valid CloseChatRoomRequest request);
}
