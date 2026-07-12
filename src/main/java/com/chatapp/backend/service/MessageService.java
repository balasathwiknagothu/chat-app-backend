package com.chatapp.backend.service;

import com.chatapp.backend.dto.ChatMessageRequest;
import com.chatapp.backend.dto.ChatMessageResponse;
import com.chatapp.backend.dto.MessageHistoryResponse;
import com.chatapp.backend.entity.Message;
import com.chatapp.backend.entity.Room;
import com.chatapp.backend.entity.User;
import com.chatapp.backend.repository.MessageRepository;
import com.chatapp.backend.repository.RoomMembershipRepository;
import com.chatapp.backend.repository.RoomRepository;
import com.chatapp.backend.repository.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class MessageService {

    private final MessageRepository messageRepository;
    private final RoomRepository roomRepository;
    private final UserRepository userRepository;
    private final RoomMembershipRepository roomMembershipRepository;

    public ChatMessageResponse sendMessage(ChatMessageRequest request, String senderUsername) {
        if (request.getContent() == null || request.getContent().isBlank()) {
            throw new IllegalArgumentException("Message content cannot be empty");
        }
        if (request.getContent().length() > 5000) {
            throw new IllegalArgumentException("Message content cannot exceed 5000 characters");
        }

        User sender = userRepository.findByUsername(senderUsername)
                .orElseThrow(() -> new EntityNotFoundException("Sender not found"));

        Room room = roomRepository.findById(request.getRoomId())
                .orElseThrow(() -> new EntityNotFoundException("Room not found"));

        boolean isMember = roomMembershipRepository.existsByUserIdAndRoomId(sender.getId(), room.getId());
        if (!isMember) {
            throw new AccessDeniedException("You are not a member of this room");
        }

        Message message = Message.builder()
                .room(room)
                .sender(sender)
                .content(request.getContent())
                .build();

        Message saved = messageRepository.save(message);

        return new ChatMessageResponse(
                saved.getId(),
                room.getId(),
                sender.getId(),
                sender.getUsername(),
                saved.getContent(),
                saved.getSentAt()
        );
    }
    public MessageHistoryResponse getRoomHistory(Long roomId, int page, int size, String requesterUsername) {
        User requester = userRepository.findByUsername(requesterUsername)
                .orElseThrow(() -> new EntityNotFoundException("User not found"));

        Room room = roomRepository.findById(roomId)
                .orElseThrow(() -> new EntityNotFoundException("Room not found"));

        boolean isMember = roomMembershipRepository.existsByUserIdAndRoomId(requester.getId(), room.getId());
        if (!isMember) {
            throw new AccessDeniedException("You are not a member of this room");
        }

        Pageable pageable = PageRequest.of(page, size, Sort.by("sentAt").descending());
        Page<Message> messagePage = messageRepository.findByRoomIdOrderBySentAtDesc(room.getId(), pageable);

        List<ChatMessageResponse> responses = messagePage.getContent().stream()
                .map(m -> new ChatMessageResponse(
                        m.getId(), room.getId(), m.getSender().getId(),
                        m.getSender().getUsername(), m.getContent(), m.getSentAt()))
                .toList();

        return new MessageHistoryResponse(
                responses,
                messagePage.getNumber(),
                messagePage.getTotalPages(),
                messagePage.getTotalElements()
        );
    }    
}