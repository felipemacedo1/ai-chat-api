package com.chatai.service;

import com.chatai.dto.request.CreateConversationRequest;
import com.chatai.dto.request.UpdateConversationRequest;
import com.chatai.dto.response.ConversationResponse;
import com.chatai.dto.response.PaginatedResponse;
import com.chatai.entity.Conversation;
import com.chatai.entity.User;
import com.chatai.exception.ResourceNotFoundException;
import com.chatai.repository.ConversationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ConversationService {
    
    private final ConversationRepository conversationRepository;
    
    @Transactional
    public ConversationResponse create(CreateConversationRequest request, User user) {
        Conversation conversation = Conversation.builder()
                .title(request != null ? request.getTitle() : null)
                .user(user)
                .build();
        
        Conversation saved = conversationRepository.save(conversation);
        return ConversationResponse.fromEntity(saved);
    }
    
    @Transactional(readOnly = true)
    public PaginatedResponse<ConversationResponse> findAll(String userId, Pageable pageable) {
        Page<Conversation> page = conversationRepository.findByUserIdOrderByUpdatedAtDesc(userId, pageable);
        return PaginatedResponse.fromPage(page, ConversationResponse::fromEntity);
    }
    
    @Transactional(readOnly = true)
    public ConversationResponse findOne(String id, String userId) {
        Conversation conversation = conversationRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Conversation", "id", id));
        return ConversationResponse.fromEntity(conversation);
    }
    
    @Transactional(readOnly = true)
    public Conversation findEntityByIdAndUserId(String id, String userId) {
        return conversationRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Conversation", "id", id));
    }
    
    @Transactional
    public ConversationResponse update(String id, UpdateConversationRequest request, String userId) {
        Conversation conversation = conversationRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Conversation", "id", id));
        
        conversation.setTitle(request.getTitle());
        Conversation saved = conversationRepository.save(conversation);
        return ConversationResponse.fromEntity(saved);
    }
    
    @Transactional
    public void delete(String id, String userId) {
        Conversation conversation = conversationRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Conversation", "id", id));
        
        conversationRepository.delete(conversation);
    }
    
    @Transactional(readOnly = true)
    public boolean existsByIdAndUserId(String id, String userId) {
        return conversationRepository.existsByIdAndUserId(id, userId);
    }
}
