package com.chatai.service;

import com.chatai.dto.request.CreateMessageRequest;
import com.chatai.dto.response.MessageResponse;
import com.chatai.entity.Conversation;
import com.chatai.entity.Message;
import com.chatai.entity.MessageRole;
import com.chatai.exception.AiServiceException;
import com.chatai.exception.ResourceNotFoundException;
import com.chatai.repository.ConversationRepository;
import com.chatai.repository.MessageRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class MessageService {
    
    private final MessageRepository messageRepository;
    private final ConversationRepository conversationRepository;
    private final AiService aiService;
    
    /**
     * Validates that message content is not empty or whitespace-only.
     * Returns true if the content is valid (non-empty after trimming).
     */
    public boolean isValidMessageContent(String content) {
        return content != null && !content.trim().isEmpty();
    }
    
    @Transactional
    public MessageResponse create(String conversationId, String userId, CreateMessageRequest request, MessageRole role) {
        // Validate content is not whitespace-only
        if (!isValidMessageContent(request.getContent())) {
            throw new IllegalArgumentException("Message content cannot be empty or whitespace-only");
        }
        
        Conversation conversation = conversationRepository.findByIdAndUserId(conversationId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Conversation", "id", conversationId));
        
        Message message = Message.builder()
                .content(request.getContent())
                .role(role)
                .conversation(conversation)
                .build();
        
        Message saved = messageRepository.save(message);
        return MessageResponse.fromEntity(saved);
    }
    
    /**
     * Creates a user message and gets AI response.
     * Also auto-generates conversation title on first AI response.
     * Requirements: 2.1, 3.5
     */
    @Transactional
    public List<MessageResponse> createWithAiResponse(String conversationId, String userId, CreateMessageRequest request) {
        // Validate content is not whitespace-only
        if (!isValidMessageContent(request.getContent())) {
            throw new IllegalArgumentException("Message content cannot be empty or whitespace-only");
        }
        
        Conversation conversation = conversationRepository.findByIdAndUserId(conversationId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Conversation", "id", conversationId));
        
        // Get conversation history for context
        List<MessageResponse> history = findByConversation(conversationId, userId);
        boolean isFirstMessage = history.isEmpty();
        
        // Save user message
        Message userMessage = Message.builder()
                .content(request.getContent())
                .role(MessageRole.USER)
                .conversation(conversation)
                .build();
        Message savedUserMessage = messageRepository.save(userMessage);
        MessageResponse userResponse = MessageResponse.fromEntity(savedUserMessage);
        
        // Add user message to history for AI context
        history.add(userResponse);
        
        // Get AI response
        String aiResponseContent;
        try {
            aiResponseContent = aiService.sendMessage(request.getContent(), history);
        } catch (AiServiceException e) {
            log.error("AI service error for conversation {}: {}", conversationId, e.getMessage());
            throw e;
        }
        
        // Save AI response
        Message aiMessage = Message.builder()
                .content(aiResponseContent)
                .role(MessageRole.ASSISTANT)
                .conversation(conversation)
                .build();
        Message savedAiMessage = messageRepository.save(aiMessage);
        MessageResponse aiResponse = MessageResponse.fromEntity(savedAiMessage);
        
        // Auto-generate title on first AI response (Requirement 3.5)
        if (isFirstMessage && (conversation.getTitle() == null || conversation.getTitle().isEmpty())) {
            try {
                String generatedTitle = aiService.generateTitle(request.getContent(), aiResponseContent);
                conversation.setTitle(generatedTitle);
                conversationRepository.save(conversation);
                log.info("Auto-generated title for conversation {}: {}", conversationId, generatedTitle);
            } catch (AiServiceException e) {
                log.warn("Failed to auto-generate title for conversation {}: {}", conversationId, e.getMessage());
                // Don't fail the request if title generation fails
            }
        }
        
        return Arrays.asList(userResponse, aiResponse);
    }
    
    @Transactional(readOnly = true)
    public List<MessageResponse> findByConversation(String conversationId, String userId) {
        // Verify user owns the conversation
        if (!conversationRepository.existsByIdAndUserId(conversationId, userId)) {
            throw new ResourceNotFoundException("Conversation", "id", conversationId);
        }
        
        List<Message> messages = messageRepository.findByConversationIdOrderByCreatedAtAsc(conversationId);
        return messages.stream()
                .map(MessageResponse::fromEntity)
                .collect(Collectors.toList());
    }
    
    @Transactional(readOnly = true)
    public long countByConversation(String conversationId) {
        return messageRepository.countByConversationId(conversationId);
    }
}
