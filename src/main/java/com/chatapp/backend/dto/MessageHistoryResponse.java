package com.chatapp.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

@Data
@AllArgsConstructor
public class MessageHistoryResponse {
    private List<ChatMessageResponse> messages;
    private int currentPage;
    private int totalPages;
    private long totalMessages;
}