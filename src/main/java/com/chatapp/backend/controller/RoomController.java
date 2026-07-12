package com.chatapp.backend.controller;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import com.chatapp.backend.dto.CreateRoomRequest;
import com.chatapp.backend.dto.RoomResponse;
import com.chatapp.backend.service.RoomService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/rooms")
@RequiredArgsConstructor
public class RoomController {

    private final RoomService roomService;

    @PostMapping
    public ResponseEntity<RoomResponse> createRoom(
            @jakarta.validation.Valid @RequestBody CreateRoomRequest request,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        RoomResponse response = roomService.createRoom(request, userDetails.getUsername());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/{roomId}/members/{username}")
    public ResponseEntity<Void> addMember(
            @PathVariable Long roomId,
            @PathVariable String username,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        roomService.addMember(roomId, username, userDetails.getUsername());
        return ResponseEntity.ok().build();
    }
}