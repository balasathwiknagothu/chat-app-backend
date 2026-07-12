package com.chatapp.backend.service;

import com.chatapp.backend.dto.LoginRequest;
import com.chatapp.backend.dto.RegisterRequest;
import com.chatapp.backend.entity.User;
import com.chatapp.backend.exception.DuplicateResourceException;
import com.chatapp.backend.repository.UserRepository;
import com.chatapp.backend.security.JwtUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtUtil jwtUtil;

    @InjectMocks
    private AuthService authService;

    private RegisterRequest registerRequest;

    @BeforeEach
    void setUp() {
        registerRequest = new RegisterRequest();
        registerRequest.setUsername("sathwik");
        registerRequest.setEmail("sathwik@example.com");
        registerRequest.setPassword("password123");
        registerRequest.setDisplayName("Sathwik");
    }

    @Test
    void register_shouldSucceed_whenUsernameAndEmailAreUnique() {
        when(userRepository.existsByUsername("sathwik")).thenReturn(false);
        when(userRepository.existsByEmail("sathwik@example.com")).thenReturn(false);
        when(passwordEncoder.encode("password123")).thenReturn("hashedPassword");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        User result = authService.register(registerRequest);

        assertThat(result.getUsername()).isEqualTo("sathwik");
        assertThat(result.getPasswordHash()).isEqualTo("hashedPassword");
        assertThat(result.getStatus()).isEqualTo("OFFLINE");
        verify(userRepository).save(any(User.class));
    }

    @Test
    void register_shouldThrow_whenUsernameAlreadyExists() {
        when(userRepository.existsByUsername("sathwik")).thenReturn(true);

        assertThatThrownBy(() -> authService.register(registerRequest))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessage("Username already taken");

        verify(userRepository, never()).save(any());
    }

    @Test
    void login_shouldSucceed_withCorrectCredentials() {
        User user = User.builder()
                .id(1L)
                .username("sathwik")
                .passwordHash("hashedPassword")
                .build();

        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setUsername("sathwik");
        loginRequest.setPassword("password123");

        when(userRepository.findByUsername("sathwik")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("password123", "hashedPassword")).thenReturn(true);
        when(jwtUtil.generateToken("sathwik")).thenReturn("fake-jwt-token");

        var response = authService.login(loginRequest);

        assertThat(response.getToken()).isEqualTo("fake-jwt-token");
        assertThat(response.getUsername()).isEqualTo("sathwik");
    }

    @Test
    void login_shouldThrow_withWrongPassword() {
        User user = User.builder()
                .username("sathwik")
                .passwordHash("hashedPassword")
                .build();

        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setUsername("sathwik");
        loginRequest.setPassword("wrongpassword");

        when(userRepository.findByUsername("sathwik")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("wrongpassword", "hashedPassword")).thenReturn(false);

        assertThatThrownBy(() -> authService.login(loginRequest))
                .isInstanceOf(BadCredentialsException.class);
    }
}