package com.pranit.connect.repository;

import com.pranit.connect.entity.Message;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface MessageRepository extends MongoRepository<Message, String> {

    Page<Message> findByRoomId(String roomId, Pageable pageable);

    Page<Message> findByRoomIdAndContentContainingIgnoreCase
            (String roomId, String keyword, Pageable pageable);
}
