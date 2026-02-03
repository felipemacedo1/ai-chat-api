package com.chatai.repository;

import com.chatai.entity.Conversation;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ConversationRepository extends JpaRepository<Conversation, String> {
    
    Page<Conversation> findByUserIdOrderByUpdatedAtDesc(String userId, Pageable pageable);
    
    Optional<Conversation> findByIdAndUserId(String id, String userId);
    
    boolean existsByIdAndUserId(String id, String userId);
}
