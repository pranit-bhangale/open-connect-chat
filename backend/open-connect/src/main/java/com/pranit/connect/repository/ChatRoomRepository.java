package com.pranit.connect.repository;

import com.pranit.connect.entity.ChatRoom;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;

import java.util.Optional;

public interface ChatRoomRepository extends MongoRepository<ChatRoom, String> {

    Optional<ChatRoom> findByRoomId(String roomId);

    boolean existsByRoomId(String roomId);


    @Query("{ '$and': [ " +
            "  { 'isClosed': { '$ne': true } }, " +
            "  { '$or': [ { 'isOpen': true }, { 'ownerId': ?0 }, { 'ownerName': ?1 }, { 'members.name': ?1 }, { 'members.memberId': ?0 } ] } " +
            "] }")
    Page<ChatRoom> findVisibleRooms(String userId, String userName, Pageable pageable);

    @Query("{ '$and': [ " +
            "  { 'isClosed': { '$ne': true } }, " +
            "  { '$or': [ { 'isOpen': true }, { 'ownerId': ?1 }, { 'ownerName': ?2 }, { 'members.name': ?2 }, { 'members.memberId': ?1 } ] }, " +
            "  { 'roomName': { '$regex': ?0, '$options': 'i' } } " +
            "] }")
    Page<ChatRoom> findVisibleRoomsWithKeyword(String keywordRegex, String userId, String userName, Pageable pageable);
}
