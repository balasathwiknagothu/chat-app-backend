package com.chatapp.backend.controller;

import com.chatapp.backend.dto.UserResponse;
import com.chatapp.backend.dto.AuthResponse;
import com.chatapp.backend.dto.LoginRequest;
import com.chatapp.backend.dto.RegisterRequest;
import com.chatapp.backend.entity.User;
import com.chatapp.backend.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    public ResponseEntity<UserResponse> register(@Valid @RequestBody RegisterRequest request) {
        User user = authService.register(request);
        UserResponse response = new UserResponse(
                user.getId(), user.getUsername(), user.getEmail(),
                user.getDisplayName(), user.getStatus()
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        AuthResponse response = authService.login(request);
        return ResponseEntity.ok(response);
    }
}