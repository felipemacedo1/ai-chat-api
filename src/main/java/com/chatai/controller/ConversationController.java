package com.chatai.controller;

import com.chatai.dto.request.CreateConversationRequest;
import com.chatai.dto.request.UpdateConversationRequest;
import com.chatai.dto.response.ConversationResponse;
import com.chatai.dto.response.PaginatedResponse;
import com.chatai.entity.User;
import com.chatai.service.ConversationService;
import com.chatai.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;

@RestController
@RequestMapping("/api/conversations")
@RequiredArgsConstructor
@Slf4j
public class ConversationController {

    private final ConversationService conversationService;
    private final UserService userService;

    /**
     * GET /api/conversations
     * Lists all conversations for the authenticated user with pagination
     * Requirements: 3.2, 6.4
     */
    @GetMapping
    public ResponseEntity<PaginatedResponse<ConversationResponse>> getAll(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int limit) {
        String userId = getCurrentUserId();
        log.info("Fetching conversations for user: {}, page: {}, limit: {}", userId, page, limit);
        
        Pageable pageable = PageRequest.of(page, limit);
        PaginatedResponse<ConversationResponse> response = conversationService.findAll(userId, pageable);
        
        log.info("Found {} conversations for user: {}", response.getData().size(), userId);
        return ResponseEntity.ok(response);
    }

    /**
     * POST /api/conversations
     * Creates a new conversation
     * Requirements: 3.1
     */
    @PostMapping
    public ResponseEntity<ConversationResponse> create(
            @Valid @RequestBody(required = false) CreateConversationRequest request) {
        String userId = getCurrentUserId();
        User user = userService.findByIdOrThrow(userId);
        
        log.info("Creating new conversation for user: {}", userId);
        ConversationResponse response = conversationService.create(
                request != null ? request : new CreateConversationRequest(), 
                user);
        
        log.info("Conversation created with id: {} for user: {}", response.getId(), userId);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * GET /api/conversations/{id}
     * Gets a specific conversation by ID
     * Requirements: 3.2
     */
    @GetMapping("/{id}")
    public ResponseEntity<ConversationResponse> getOne(@PathVariable String id) {
        String userId = getCurrentUserId();
        log.info("Fetching conversation: {} for user: {}", id, userId);
        
        ConversationResponse response = conversationService.findOne(id, userId);
        
        log.info("Found conversation: {} for user: {}", id, userId);
        return ResponseEntity.ok(response);
    }

    /**
     * PATCH /api/conversations/{id}
     * Updates a conversation (rename)
     * Requirements: 3.4
     */
    @PatchMapping("/{id}")
    public ResponseEntity<ConversationResponse> update(
            @PathVariable String id,
            @Valid @RequestBody UpdateConversationRequest request) {
        String userId = getCurrentUserId();
        log.info("Updating conversation: {} for user: {}", id, userId);
        
        ConversationResponse response = conversationService.update(id, request, userId);
        
        log.info("Conversation updated: {} for user: {}", id, userId);
        return ResponseEntity.ok(response);
    }

    /**
     * DELETE /api/conversations/{id}
     * Deletes a conversation and all its messages
     * Requirements: 3.3
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable String id) {
        String userId = getCurrentUserId();
        log.info("Deleting conversation: {} for user: {}", id, userId);
        
        conversationService.delete(id, userId);
        
        log.info("Conversation deleted: {} for user: {}", id, userId);
        return ResponseEntity.noContent().build();
    }

    private String getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication.getName();
    }
}
