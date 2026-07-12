package com.chatapp.backend.websocket;

import com.chatapp.backend.dto.ChatMessageRequest;
import com.chatapp.backend.dto.ChatMessageResponse;
import com.chatapp.backend.service.MessageService;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

import java.security.Principal;

@Controller
@RequiredArgsConstructor
public class ChatController {

    private final MessageService messageService;
    private final SimpMessagingTemplate messagingTemplate;

    @MessageMapping("/chat.send")
    public void sendMessage(ChatMessageRequest request, Principal principal) {
        ChatMessageResponse response = messageService.sendMessage(request, principal.getName());

        messagingTemplate.convertAndSend(
                "/topic/room." + request.getRoomId(),
                response
        );
    }
}