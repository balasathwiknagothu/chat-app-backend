package com.chatapp.backend.repository;

import com.chatapp.backend.entity.Message;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MessageRepository extends JpaRepository<Message, Long> {

    Page<Message> findByRoomIdOrderBySentAtDesc(Long roomId, Pageable pageable);
}