package com.chatai.controller;

import com.chatai.dto.request.CreateMessageRequest;
import com.chatai.dto.response.MessageResponse;
import com.chatai.service.MessageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.List;

@RestController
@RequestMapping("/api/conversations/{conversationId}/messages")
@RequiredArgsConstructor
@Slf4j
public class MessageController {

    private final MessageService messageService;

    /**
     * GET /api/conversations/{conversationId}/messages
     * Gets all messages for a conversation in chronological order
     * Requirements: 5.1, 5.3
     */
    @GetMapping
    public ResponseEntity<List<MessageResponse>> getMessages(@PathVariable String conversationId) {
        String userId = getCurrentUserId();
        log.info("Fetching messages for conversation: {} by user: {}", conversationId, userId);
        
        List<MessageResponse> messages = messageService.findByConversation(conversationId, userId);
        
        log.info("Found {} messages for conversation: {}", messages.size(), conversationId);
        return ResponseEntity.ok(messages);
    }

    /**
     * POST /api/conversations/{conversationId}/messages
     * Creates a new user message and gets AI response
     * Requirements: 2.1, 3.5, 5.1
     */
    @PostMapping
    public ResponseEntity<List<MessageResponse>> createMessage(
            @PathVariable String conversationId,
            @Valid @RequestBody CreateMessageRequest request) {
        String userId = getCurrentUserId();
        log.info("Creating message in conversation: {} by user: {}", conversationId, userId);
        
        // Create user message and get AI response
        List<MessageResponse> responses = messageService.createWithAiResponse(conversationId, userId, request);
        
        log.info("Created {} messages in conversation: {}", responses.size(), conversationId);
        return ResponseEntity.status(HttpStatus.CREATED).body(responses);
    }

    private String getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication.getName();
    }
}
