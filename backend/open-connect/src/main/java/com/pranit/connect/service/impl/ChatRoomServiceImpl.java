package com.pranit.connect.service.impl;

import com.pranit.connect.dto.ChatRoomResponse;
import com.pranit.connect.dto.CloseChatRoomRequest;
import com.pranit.connect.dto.CreateChatRoomRequest;
import com.pranit.connect.dto.JoinChatRoomRequest;
import com.pranit.connect.dto.LeaveChatRequest;
import com.pranit.connect.dto.MessageRequest;
import com.pranit.connect.dto.MessageResponse;
import com.pranit.connect.dto.OpenChatRequest;
import com.pranit.connect.entity.ChatMember;
import com.pranit.connect.entity.ChatRoom;
import com.pranit.connect.entity.Message;
import com.pranit.connect.entity.MessageType;
import com.pranit.connect.exception.ResourceNotFoundException;
import com.pranit.connect.exception.RoomAlreadyExistsException;
import com.pranit.connect.exception.RoomClosedException;
import com.pranit.connect.exception.UnauthorizedException;
import com.pranit.connect.repository.ChatRoomRepository;
import com.pranit.connect.repository.MessageRepository;
import com.pranit.connect.service.ChatRoomService;
import com.pranit.connect.wrapper.PageResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import java.util.regex.Pattern;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChatRoomServiceImpl implements ChatRoomService {

    private final ChatRoomRepository chatRoomRepository;
    private final MessageRepository messageRepository;

    public static String generateChatRoomId() {
        return String.format("%06d", ThreadLocalRandom.current().nextInt(1_000_000));
    }

    public static String generateMemberId() {
        return UUID.randomUUID().toString();
    }

    @Override
    public ChatRoomResponse createChatRoom(final CreateChatRoomRequest request) {
        final String roomId = generateChatRoomId();
        if (this.chatRoomRepository.existsByRoomId(roomId)) {
            throw new RoomAlreadyExistsException("Room with ID " + roomId + " already exists");
        }
        final String memberId = (request.ownerId() != null && !request.ownerId().isBlank())
                ? request.ownerId() : generateMemberId();
        final ChatMember owner = ChatMember.builder()
                .memberId(memberId)
                .name(request.owner())
                .build();
        final boolean isOpen = request.isOpen() == null || request.isOpen();
        final ChatRoom room = ChatRoom.builder()
                .roomId(roomId)
                .roomName(request.roomName())
                .ownerId(memberId)
                .ownerName(request.owner())
                .isOpen(isOpen)
                .isClosed(false)
                .createdAt(Instant.now())
                .members(new ArrayList<>(List.of(owner)))
                .build();
        this.chatRoomRepository.save(room);
        log.info("Chat room created: id: {}, name: {}, owner: {}, isOpen: {}",
                roomId, request.roomName(), request.owner(), isOpen);
        return ChatRoomResponse.builder()
                .roomId(roomId)
                .roomName(room.getRoomName())
                .ownerId(memberId)
                .ownerName(room.getOwnerName())
                .memberId(memberId)
                .member(owner.getName())
                .memberCount(room.getMembers().size())
                .isOpen(room.isOpen())
                .isClosed(false)
                .createdAt(room.getCreatedAt())
                .build();
    }

    @Override
    public ChatRoomResponse joinChatRoom(final JoinChatRoomRequest request) {
        final ChatRoom room = this.chatRoomRepository.findByRoomId(request.roomId())
                .orElseThrow(() -> new ResourceNotFoundException("Room not found: " + request.roomId()));
        if (room.isClosed()) {
            throw new RoomClosedException("Chat room #" + room.getRoomId() + " has been permanently closed by the owner and cannot be joined.");
        }
        final ChatMember existingMember = room.getMembers()
                .stream()
                .filter(m -> m.getName()
                        .equalsIgnoreCase(request.name().trim()))
                .findFirst()
                .orElse(null);
        String memberId;
        boolean roomChanged = false;
        if (existingMember != null) {
            memberId = existingMember.getMemberId();
        } else {
            memberId = (request.memberId() != null && !request.memberId().isBlank())
                    ? request.memberId() : generateMemberId();
            final ChatMember newMember = ChatMember.builder()
                    .memberId(memberId)
                    .name(request.name().trim())
                    .build();
            room.getMembers().add(newMember);
            roomChanged = true;
            log.info("Member {} joined room {}", request.name(), room.getRoomId());
        }
        if (roomChanged) {
            this.chatRoomRepository.save(room);
        }
        return ChatRoomResponse.builder()
                .roomId(room.getRoomId())
                .roomName(room.getRoomName())
                .ownerId(room.getOwnerId())
                .ownerName(room.getOwnerName())
                .memberId(memberId)
                .member(request.name().trim())
                .memberCount(room.getMembers().size())
                .isOpen(room.isOpen())
                .isClosed(room.isClosed())
                .createdAt(room.getCreatedAt())
                .closedAt(room.getClosedAt())
                .build();
    }

    @Override
    public ChatRoomResponse getChatRoom(final String roomId) {
        final ChatRoom room = this.chatRoomRepository.findByRoomId(roomId)
                .orElseThrow(() -> new ResourceNotFoundException("Room not found: " + roomId));
        return ChatRoomResponse.builder()
                .roomId(room.getRoomId())
                .roomName(room.getRoomName())
                .ownerId(room.getOwnerId())
                .ownerName(room.getOwnerName())
                .memberCount(room.getMembers() != null ? room.getMembers().size() : 0)
                .isOpen(room.isOpen())
                .isClosed(room.isClosed())
                .createdAt(room.getCreatedAt())
                .closedAt(room.getClosedAt())
                .build();
    }

    @Override
    public PageResponse<ChatRoomResponse> getOpenRooms
            (final int page, final int size, final String keyword) {
        return getRooms(page, size, keyword, null, null);
    }

    @Override
    public PageResponse<ChatRoomResponse> getRooms
            (final int page, final int size,
             final String keyword, final String userId, final String userName) {
        final Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        final Page<ChatRoom> roomPage;
        final String cleanUserId = userId != null ? userId.trim() : "";
        final String cleanUserName = userName != null ? userName.trim() : "";
        if (keyword == null || keyword.isBlank()) {
            roomPage = this.chatRoomRepository.findVisibleRooms(cleanUserId, cleanUserName, pageable);
        } else {
            final String pattern = ".*" + Pattern.quote(keyword.trim()) + ".*";
            roomPage = this.chatRoomRepository.findVisibleRoomsWithKeyword(pattern, cleanUserId, cleanUserName, pageable);
        }
        if (roomPage == null) {
            return PageResponse.<ChatRoomResponse>builder()
                    .contents(List.of())
                    .currentPage(page)
                    .pageSize(size)
                    .totalElements(0)
                    .totalPages(0)
                    .isFirstPage(true)
                    .isLastPage(true)
                    .build();
        }
        return PageResponse.<ChatRoomResponse>builder()
                .contents(roomPage.getContent().stream()
                        .map(r -> ChatRoomResponse.builder()
                                .roomId(r.getRoomId())
                                .roomName(r.getRoomName())
                                .ownerId(r.getOwnerId())
                                .ownerName(r.getOwnerName() != null ? r.getOwnerName() : (r.getMembers() != null && !r.getMembers().isEmpty() ? r.getMembers().get(0).getName() : "Owner"))
                                .member(r.getOwnerName() != null ? r.getOwnerName() : (r.getMembers() != null && !r.getMembers().isEmpty() ? r.getMembers().get(0).getName() : "Owner"))
                                .memberCount(r.getMembers() != null ? r.getMembers().size() : 0)
                                .isOpen(r.isOpen())
                                .isClosed(r.isClosed())
                                .createdAt(r.getCreatedAt())
                                .closedAt(r.getClosedAt())
                                .build())
                        .toList())
                .currentPage(roomPage.getNumber())
                .pageSize(roomPage.getSize())
                .totalElements(roomPage.getTotalElements())
                .totalPages(roomPage.getTotalPages())
                .isFirstPage(roomPage.isFirst())
                .isLastPage(roomPage.isLast())
                .build();
    }

    @Override
    public PageResponse<MessageResponse> getMessages(
            final String roomId, final int page, final int size, final String keyword) {
        if (!this.chatRoomRepository.existsByRoomId(roomId)) {
            throw new ResourceNotFoundException("Room not found: " + roomId);
        }

        final Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "timeStamp"));
        final Page<Message> messagePage;
        if (keyword == null || keyword.isBlank()) {
            messagePage = this.messageRepository.findByRoomId(roomId, pageable);
        } else {
            messagePage = this.messageRepository.findByRoomIdAndContentContainingIgnoreCase(roomId, keyword.trim(), pageable);
        }

        return PageResponse.<MessageResponse>builder()
                .contents(messagePage.getContent().stream()
                        .map(this::mapToResponse)
                        .toList())
                .currentPage(messagePage.getNumber())
                .pageSize(messagePage.getSize())
                .totalElements(messagePage.getTotalElements())
                .totalPages(messagePage.getTotalPages())
                .isFirstPage(messagePage.isFirst())
                .isLastPage(messagePage.isLast())
                .build();
    }

    @Override
    public MessageResponse sendMessage(final String roomId, final MessageRequest request) {
        final ChatRoom room = this.chatRoomRepository.findByRoomId(roomId)
                .orElseThrow(() -> new ResourceNotFoundException("Room not found: " + roomId));
        if (room.isClosed()) {
            throw new RoomClosedException("Chat room #" + roomId + " is closed. No further messages can be sent.");
        }

        String senderName = request.senderName();
        if (!room.isOpen()) {
            final ChatMember sender = room.getMembers()
                    .stream()
                    .filter(m -> m.getMemberId().equals(request.senderId())
                            || (request.senderName() != null && !request.senderName().isBlank() && m.getName().equalsIgnoreCase(request.senderName())))
                    .findFirst()
                    .orElseThrow(() -> new ResourceNotFoundException("User is not an authorized member of this private room"));
            if (senderName == null || senderName.isBlank()) {
                senderName = sender.getName();
            }
        } else {
            if (senderName == null || senderName.isBlank()) {
                senderName = room.getMembers().stream()
                        .filter(m -> m.getMemberId().equals(request.senderId()))
                        .map(ChatMember::getName)
                        .findFirst()
                        .orElse("Participant");
            }
            final String effectiveSenderName = senderName;
            boolean isMember = room.getMembers().stream()
                    .anyMatch(m -> m.getMemberId().equals(request.senderId()) || m.getName().equalsIgnoreCase(effectiveSenderName));
            if (!isMember) {
                room.getMembers().add(ChatMember.builder()
                        .memberId(request.senderId())
                        .name(effectiveSenderName)
                        .build());
                this.chatRoomRepository.save(room);
            }
        }

        final MessageType messageType = request.type() != null ? request.type() : MessageType.CHAT;

        final Message message = Message.builder()
                .roomId(roomId)
                .senderId(request.senderId())
                .senderName(senderName)
                .content(request.content())
                .type(messageType)
                .timeStamp(Instant.now())
                .build();

        messageRepository.save(message);

        return MessageResponse.builder()
                .messageId(message.getId())
                .roomId(message.getRoomId())
                .senderId(message.getSenderId())
                .senderName(senderName)
                .content(message.getContent())
                .type(message.getType())
                .timeStamp(message.getTimeStamp())
                .build();
    }

    @Override
    public MessageResponse openChat(final String roomId, final OpenChatRequest request) {
        final ChatRoom room = chatRoomRepository.findByRoomId(roomId)
                .orElseThrow(() -> new ResourceNotFoundException("Room not found: " + roomId));
        if (room.isClosed()) {
            throw new RoomClosedException("Chat room #" + roomId + " has been closed by the owner.");
        }
        boolean exists = room.getMembers().stream()
                .anyMatch(m -> m.getMemberId().equals(request.senderId()) || m.getName().equalsIgnoreCase(request.senderName()));
        if (!exists) {
            room.getMembers().add(ChatMember.builder()
                    .memberId(request.senderId())
                    .name(request.senderName())
                    .build());
            chatRoomRepository.save(room);
        }
        final Message message = Message.builder()
                .roomId(roomId)
                .senderId(request.senderId())
                .senderName(request.senderName())
                .content(request.senderName() + " joined the chat")
                .type(MessageType.JOIN)
                .timeStamp(Instant.now())
                .build();

        messageRepository.save(message);

        return MessageResponse.builder()
                .messageId(message.getId())
                .roomId(roomId)
                .senderId(request.senderId())
                .senderName(request.senderName())
                .content(message.getContent())
                .type(MessageType.JOIN)
                .timeStamp(message.getTimeStamp())
                .build();
    }

    @Override
    public MessageResponse leaveChat(final String roomId, final LeaveChatRequest request) {
        final ChatRoom room = chatRoomRepository.findByRoomId(roomId)
                .orElseThrow(() -> new ResourceNotFoundException("Room not found: " + roomId));
        boolean isOwner = (request.senderId() != null && request.senderId().equals(room.getOwnerId()))
                || (room.getOwnerName() != null && room.getOwnerName().equalsIgnoreCase(request.senderName()));
        if (isOwner) {
            log.info("Owner {} exited chat view for room {} - room remains intact",
                    request.senderName(), roomId);
            return null;
        }
        // Regular member leaves: remove from room members list
        if (room.getMembers() != null) {
            boolean removed = room.getMembers().removeIf(m ->
                    (request.senderId() != null && m.getMemberId().equals(request.senderId()))
                            || (request.senderName() != null && m.getName().equalsIgnoreCase(request.senderName())));
            if (removed) {
                chatRoomRepository.save(room);
            }
        }
        final Message message = Message.builder()
                .roomId(roomId)
                .senderId(request.senderId())
                .senderName(request.senderName())
                .content(request.senderName() + " left the chat")
                .type(MessageType.LEAVE)
                .timeStamp(Instant.now())
                .build();
        messageRepository.save(message);
        return MessageResponse.builder()
                .messageId(message.getId())
                .roomId(roomId)
                .senderId(request.senderId())
                .senderName(request.senderName())
                .content(message.getContent())
                .type(MessageType.LEAVE)
                .timeStamp(message.getTimeStamp())
                .build();
    }

    @Override
    public MessageResponse closeChatRoom(final String roomId, final CloseChatRoomRequest request) {
        final ChatRoom room = chatRoomRepository.findByRoomId(roomId)
                .orElseThrow(() -> new ResourceNotFoundException("Room not found: " + roomId));

        if (room.isClosed()) {
            throw new RoomClosedException("Chat room #" + roomId + " is already closed.");
        }
        // ONLY the room owner can close the room!
        boolean isOwner = (request.userId() != null && !request.userId().isBlank() && request.userId().equals(room.getOwnerId()))
                || (room.getOwnerName() != null && room.getOwnerName().equalsIgnoreCase(request.userName().trim()));
        if (!isOwner) {
            throw new UnauthorizedException("Only the room owner (" + room.getOwnerName() + ") can close this chat room.");
        }

        // Mark room permanently closed
        room.setClosed(true);
        room.setClosedAt(Instant.now());
        room.setClosedBy(request.userName().trim());
        chatRoomRepository.save(room);

        log.info("Chat room {} permanently closed by owner {}", roomId, room.getOwnerName());
        final Message message = Message.builder()
                .roomId(roomId)
                .senderId(room.getOwnerId())
                .senderName(room.getOwnerName())
                .content("Chat room #" + roomId + " was permanently closed by owner " + room.getOwnerName())
                .type(MessageType.ROOM_CLOSED)
                .timeStamp(Instant.now())
                .build();

        messageRepository.save(message);

        return MessageResponse.builder()
                .messageId(message.getId())
                .roomId(roomId)
                .senderId(room.getOwnerId())
                .senderName(room.getOwnerName())
                .content(message.getContent())
                .type(MessageType.ROOM_CLOSED)
                .timeStamp(message.getTimeStamp())
                .build();
    }

    private MessageResponse mapToResponse(final Message message) {
        return MessageResponse.builder()
                .messageId(message.getId())
                .roomId(message.getRoomId())
                .senderId(message.getSenderId())
                .senderName(message.getSenderName() != null ? message.getSenderName() : "Unknown")
                .content(message.getContent())
                .type(message.getType())
                .timeStamp(message.getTimeStamp())
                .build();
    }
}
