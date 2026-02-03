package com.chatai.service;

import com.chatai.dto.request.CreateMessageRequest;
import com.chatai.dto.response.MessageResponse;
import com.chatai.entity.Conversation;
import com.chatai.entity.Message;
import com.chatai.entity.MessageRole;
import com.chatai.exception.ResourceNotFoundException;
import com.chatai.repository.ConversationRepository;
import com.chatai.repository.MessageRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MessageService {
    
    private final MessageRepository messageRepository;
    private final ConversationRepository conversationRepository;
    
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
