package com.chatapp.backend.service;

import com.chatapp.backend.entity.RoomType;
import org.springframework.security.access.AccessDeniedException;
import com.chatapp.backend.dto.CreateRoomRequest;
import com.chatapp.backend.dto.RoomResponse;
import com.chatapp.backend.entity.Room;
import com.chatapp.backend.entity.RoomMembership;
import com.chatapp.backend.entity.User;
import com.chatapp.backend.repository.RoomMembershipRepository;
import com.chatapp.backend.repository.RoomRepository;
import com.chatapp.backend.repository.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class RoomService {

    private final RoomRepository roomRepository;
    private final RoomMembershipRepository roomMembershipRepository;
    private final UserRepository userRepository;

    public RoomResponse createRoom(CreateRoomRequest request, String creatorUsername) {
        User creator = userRepository.findByUsername(creatorUsername)
                .orElseThrow(() -> new EntityNotFoundException("User not found"));

        Room room = Room.builder()
                .name(request.getName())
                .type(RoomType.valueOf(request.getType()))
                .build();
        Room saved = roomRepository.save(room);

        RoomMembership membership = RoomMembership.builder()
                .user(creator)
                .room(saved)
                .build();
        roomMembershipRepository.save(membership);

        return new RoomResponse(saved.getId(), saved.getName(), saved.getType().name());
    }

    public void addMember(Long roomId, String usernameToAdd, String requesterUsername) {
        Room room = roomRepository.findById(roomId)
                .orElseThrow(() -> new EntityNotFoundException("Room not found"));

        User requester = userRepository.findByUsername(requesterUsername)
                .orElseThrow(() -> new EntityNotFoundException("Requester not found"));

        boolean requesterIsMember = roomMembershipRepository.existsByUserIdAndRoomId(requester.getId(), room.getId());
        if (!requesterIsMember) {
            throw new AccessDeniedException("You must be a member of this room to add others");
        }

        User user = userRepository.findByUsername(usernameToAdd)
                .orElseThrow(() -> new EntityNotFoundException("User not found"));

        if (roomMembershipRepository.existsByUserIdAndRoomId(user.getId(), room.getId())) {
            return;
        }

        RoomMembership membership = RoomMembership.builder()
                .user(user)
                .room(room)
                .build();
        roomMembershipRepository.save(membership);
    }
}