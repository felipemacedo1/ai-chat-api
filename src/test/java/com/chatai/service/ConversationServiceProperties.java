package com.chatai.service;

import com.chatai.dto.request.CreateConversationRequest;
import com.chatai.dto.response.ConversationResponse;
import com.chatai.entity.Conversation;
import com.chatai.entity.Message;
import com.chatai.entity.MessageRole;
import com.chatai.entity.User;
import com.chatai.repository.ConversationRepository;
import com.chatai.repository.MessageRepository;
import com.chatai.repository.UserRepository;
import net.jqwik.api.*;
import net.jqwik.api.constraints.AlphaChars;
import net.jqwik.api.constraints.IntRange;
import net.jqwik.api.constraints.StringLength;
import net.jqwik.api.lifecycle.BeforeProperty;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Property-based tests for ConversationService
 * 
 * **Feature: ai-chat-interface, Property 8: New conversation is empty**
 * **Feature: ai-chat-interface, Property 9: Conversation deletion cascades to messages**
 * **Feature: ai-chat-interface, Property 10: Conversation rename persists**
 */
public class ConversationServiceProperties {

    private static ConfigurableApplicationContext context;
    private ConversationService conversationService;
    private ConversationRepository conversationRepository;
    private MessageRepository messageRepository;
    private UserRepository userRepository;
    private PasswordEncoder passwordEncoder;
    private TransactionTemplate transactionTemplate;

    @BeforeProperty
    void setUp() {
        if (context == null) {
            System.setProperty("spring.profiles.active", "test");
            context = SpringApplication.run(com.chatai.ChatAiApplication.class);
        }
        conversationService = context.getBean(ConversationService.class);
        conversationRepository = context.getBean(ConversationRepository.class);
        messageRepository = context.getBean(MessageRepository.class);
        userRepository = context.getBean(UserRepository.class);
        passwordEncoder = context.getBean(PasswordEncoder.class);
        transactionTemplate = context.getBean(TransactionTemplate.class);
        // Clean database before each property
        transactionTemplate.execute(status -> {
            conversationRepository.deleteAll();
            userRepository.deleteAll();
            return null;
        });
    }

    private User createTestUser() {
        // Use UUID to ensure unique email for each test run
        String uniqueId = UUID.randomUUID().toString().substring(0, 8);
        String email = "user_" + uniqueId + "@example.com";
        User user = User.builder()
                .email(email)
                .password(passwordEncoder.encode("password123"))
                .name("Test User " + uniqueId)
                .build();
        return userRepository.save(user);
    }

    /**
     * **Feature: ai-chat-interface, Property 8: New conversation is empty**
     * **Validates: Requirements 3.1**
     * 
     * For any newly created conversation, the conversation SHALL have zero messages 
     * and a null or auto-generated title.
     */
    @Property(tries = 100)
    void newConversationIsEmpty() {
        // Arrange
        User user = transactionTemplate.execute(status -> createTestUser());
        CreateConversationRequest request = new CreateConversationRequest();

        // Act
        ConversationResponse response = conversationService.create(request, user);

        // Assert - conversation was created
        assertThat(response).isNotNull();
        assertThat(response.getId()).isNotNull();
        
        // Assert - conversation has zero messages (from DTO)
        assertThat(response.getMessageCount()).isEqualTo(0);
        
        // Assert - title is null (no auto-generated title yet)
        assertThat(response.getTitle()).isNull();
        
        // Verify in database within transaction to avoid lazy loading issues
        transactionTemplate.execute(status -> {
            Conversation saved = conversationRepository.findById(response.getId()).orElse(null);
            assertThat(saved).isNotNull();
            assertThat(saved.getMessages()).isEmpty();
            return null;
        });
    }

    /**
     * **Feature: ai-chat-interface, Property 8: New conversation is empty**
     * **Validates: Requirements 3.1**
     * 
     * For any newly created conversation with an optional title, the conversation 
     * SHALL have zero messages and the provided title.
     */
    @Property(tries = 100)
    void newConversationWithTitleIsEmpty(
            @ForAll @AlphaChars @StringLength(min = 1, max = 50) String title
    ) {
        // Arrange
        User user = transactionTemplate.execute(status -> createTestUser());
        CreateConversationRequest request = CreateConversationRequest.builder()
                .title(title)
                .build();

        // Act
        ConversationResponse response = conversationService.create(request, user);

        // Assert - conversation was created with title
        assertThat(response).isNotNull();
        assertThat(response.getId()).isNotNull();
        assertThat(response.getTitle()).isEqualTo(title);
        
        // Assert - conversation has zero messages (from DTO)
        assertThat(response.getMessageCount()).isEqualTo(0);
        
        // Verify in database within transaction to avoid lazy loading issues
        transactionTemplate.execute(status -> {
            Conversation saved = conversationRepository.findById(response.getId()).orElse(null);
            assertThat(saved).isNotNull();
            assertThat(saved.getMessages()).isEmpty();
            assertThat(saved.getTitle()).isEqualTo(title);
            return null;
        });
    }

    /**
     * **Feature: ai-chat-interface, Property 9: Conversation deletion cascades to messages**
     * **Validates: Requirements 3.3**
     * 
     * For any conversation with associated messages, deleting the conversation 
     * SHALL also delete all associated messages from the database.
     */
    @Property(tries = 100)
    void conversationDeletionCascadesToMessages(
            @ForAll @IntRange(min = 1, max = 5) int messageCount
    ) {
        // Arrange - create user and conversation with messages
        AtomicReference<String> conversationIdRef = new AtomicReference<>();
        AtomicReference<String> userIdRef = new AtomicReference<>();
        
        transactionTemplate.execute(status -> {
            User user = createTestUser();
            userIdRef.set(user.getId());
            
            Conversation conversation = Conversation.builder()
                    .title("Test Conversation")
                    .user(user)
                    .build();
            conversation = conversationRepository.save(conversation);
            conversationIdRef.set(conversation.getId());
            
            // Add messages to the conversation
            for (int i = 0; i < messageCount; i++) {
                Message message = Message.builder()
                        .content("Message " + i)
                        .role(i % 2 == 0 ? MessageRole.USER : MessageRole.ASSISTANT)
                        .conversation(conversation)
                        .build();
                messageRepository.save(message);
            }
            return null;
        });
        
        String conversationId = conversationIdRef.get();
        String userId = userIdRef.get();
        
        // Verify messages exist before deletion
        long messageCountBefore = messageRepository.countByConversationId(conversationId);
        assertThat(messageCountBefore).isEqualTo(messageCount);
        
        // Act - delete the conversation
        conversationService.delete(conversationId, userId);
        
        // Assert - conversation is deleted
        assertThat(conversationRepository.findById(conversationId)).isEmpty();
        
        // Assert - all messages are also deleted (cascade)
        long messageCountAfter = messageRepository.countByConversationId(conversationId);
        assertThat(messageCountAfter).isEqualTo(0);
    }

    /**
     * **Feature: ai-chat-interface, Property 10: Conversation rename persists**
     * **Validates: Requirements 3.4**
     * 
     * For any conversation and any new valid title, renaming the conversation 
     * SHALL result in the title being updated and persisted.
     */
    @Property(tries = 100)
    void conversationRenamePersists(
            @ForAll @AlphaChars @StringLength(min = 1, max = 50) String originalTitle,
            @ForAll @AlphaChars @StringLength(min = 1, max = 50) String newTitle
    ) {
        // Arrange - create user and conversation with original title
        AtomicReference<String> conversationIdRef = new AtomicReference<>();
        AtomicReference<String> userIdRef = new AtomicReference<>();
        
        transactionTemplate.execute(status -> {
            User user = createTestUser();
            userIdRef.set(user.getId());
            
            Conversation conversation = Conversation.builder()
                    .title(originalTitle)
                    .user(user)
                    .build();
            conversation = conversationRepository.save(conversation);
            conversationIdRef.set(conversation.getId());
            return null;
        });
        
        String conversationId = conversationIdRef.get();
        String userId = userIdRef.get();
        
        // Act - rename the conversation
        com.chatai.dto.request.UpdateConversationRequest updateRequest = 
                com.chatai.dto.request.UpdateConversationRequest.builder()
                        .title(newTitle)
                        .build();
        ConversationResponse response = conversationService.update(conversationId, updateRequest, userId);
        
        // Assert - response has new title
        assertThat(response.getTitle()).isEqualTo(newTitle);
        
        // Assert - title is persisted in database
        transactionTemplate.execute(status -> {
            Conversation saved = conversationRepository.findById(conversationId).orElse(null);
            assertThat(saved).isNotNull();
            assertThat(saved.getTitle()).isEqualTo(newTitle);
            return null;
        });
    }

    /**
     * **Feature: ai-chat-interface, Property 15: Pagination metadata is accurate**
     * **Validates: Requirements 6.4**
     * 
     * For any paginated response, the metadata (total, page, limit, totalPages) 
     * SHALL accurately reflect the actual data in the database.
     */
    @Property(tries = 100)
    void paginationMetadataIsAccurate(
            @ForAll @IntRange(min = 1, max = 10) int totalConversations,
            @ForAll @IntRange(min = 1, max = 5) int pageSize
    ) {
        // Arrange - create user and multiple conversations
        AtomicReference<String> userIdRef = new AtomicReference<>();
        
        transactionTemplate.execute(status -> {
            User user = createTestUser();
            userIdRef.set(user.getId());
            
            // Create multiple conversations
            for (int i = 0; i < totalConversations; i++) {
                Conversation conversation = Conversation.builder()
                        .title("Conversation " + i)
                        .user(user)
                        .build();
                conversationRepository.save(conversation);
            }
            return null;
        });
        
        String userId = userIdRef.get();
        
        // Calculate expected pagination values
        int expectedTotalPages = (int) Math.ceil((double) totalConversations / pageSize);
        
        // Act - fetch first page
        org.springframework.data.domain.Pageable pageable = 
                org.springframework.data.domain.PageRequest.of(0, pageSize);
        com.chatai.dto.response.PaginatedResponse<ConversationResponse> response = 
                conversationService.findAll(userId, pageable);
        
        // Assert - metadata is accurate
        assertThat(response.getMeta().getTotal()).isEqualTo(totalConversations);
        assertThat(response.getMeta().getPage()).isEqualTo(0);
        assertThat(response.getMeta().getLimit()).isEqualTo(pageSize);
        assertThat(response.getMeta().getTotalPages()).isEqualTo(expectedTotalPages);
        
        // Assert - data size matches expected for first page
        int expectedDataSize = Math.min(pageSize, totalConversations);
        assertThat(response.getData().size()).isEqualTo(expectedDataSize);
    }
}
