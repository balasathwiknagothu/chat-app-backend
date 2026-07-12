package com.chatapp.backend.repository;

import com.chatapp.backend.entity.RoomMembership;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RoomMembershipRepository extends JpaRepository<RoomMembership, Long> {

    List<RoomMembership> findByUserId(Long userId);

    List<RoomMembership> findByRoomId(Long roomId);

    boolean existsByUserIdAndRoomId(Long userId, Long roomId);
}