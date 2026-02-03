package com.chatai.service;

import com.chatai.dto.request.CreateMessageRequest;
import com.chatai.dto.response.MessageResponse;
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

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Property-based tests for MessageService
 * 
 * **Feature: ai-chat-interface, Property 12: Messages retrieved in chronological order**
 * **Feature: ai-chat-interface, Property 11: Message persistence round-trip**
 * **Feature: ai-chat-interface, Property 6: Whitespace-only messages are rejected**
 */
public class MessageServiceProperties {

    private static ConfigurableApplicationContext context;
    private MessageService messageService;
    private MessageRepository messageRepository;
    private ConversationRepository conversationRepository;
    private UserRepository userRepository;
    private PasswordEncoder passwordEncoder;
    private TransactionTemplate transactionTemplate;

    @BeforeProperty
    void setUp() {
        if (context == null) {
            System.setProperty("spring.profiles.active", "test");
            context = SpringApplication.run(com.chatai.ChatAiApplication.class);
        }
        messageService = context.getBean(MessageService.class);
        messageRepository = context.getBean(MessageRepository.class);
        conversationRepository = context.getBean(ConversationRepository.class);
        userRepository = context.getBean(UserRepository.class);
        passwordEncoder = context.getBean(PasswordEncoder.class);
        transactionTemplate = context.getBean(TransactionTemplate.class);
        // Clean database before each property
        transactionTemplate.execute(status -> {
            messageRepository.deleteAll();
            conversationRepository.deleteAll();
            userRepository.deleteAll();
            return null;
        });
    }

    private User createTestUser() {
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
     * **Feature: ai-chat-interface, Property 12: Messages retrieved in chronological order**
     * **Validates: Requirements 5.3**
     * 
     * For any conversation with multiple messages, retrieving the conversation 
     * SHALL return messages sorted by creation timestamp in ascending order.
     */
    @Property(tries = 100)
    void messagesRetrievedInChronologicalOrder(
            @ForAll @IntRange(min = 2, max = 10) int messageCount
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
            
            // Add messages to the conversation with slight delays to ensure different timestamps
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
        
        // Act - retrieve messages
        List<MessageResponse> messages = messageService.findByConversation(conversationId, userId);
        
        // Assert - messages are in chronological order
        assertThat(messages).hasSize(messageCount);
        
        for (int i = 0; i < messages.size() - 1; i++) {
            LocalDateTime current = messages.get(i).getCreatedAt();
            LocalDateTime next = messages.get(i + 1).getCreatedAt();
            assertThat(current).isBeforeOrEqualTo(next);
        }
    }

    /**
     * **Feature: ai-chat-interface, Property 11: Message persistence round-trip**
     * **Validates: Requirements 5.4, 5.5**
     * 
     * For any valid message object, serializing to JSON and deserializing back 
     * SHALL produce an equivalent message object with the same content, role, and metadata.
     */
    @Property(tries = 100)
    void messagePersistenceRoundTrip(
            @ForAll @AlphaChars @StringLength(min = 1, max = 200) String content,
            @ForAll MessageRole role
    ) {
        // Arrange - create user and conversation
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
            return null;
        });
        
        String conversationId = conversationIdRef.get();
        String userId = userIdRef.get();
        
        // Act - create message via service (simulates serialization to DB)
        CreateMessageRequest request = CreateMessageRequest.builder()
                .content(content)
                .build();
        MessageResponse created = messageService.create(conversationId, userId, request, role);
        
        // Act - retrieve message (simulates deserialization from DB)
        List<MessageResponse> messages = messageService.findByConversation(conversationId, userId);
        
        // Assert - round-trip preserves data
        assertThat(messages).hasSize(1);
        MessageResponse retrieved = messages.get(0);
        
        assertThat(retrieved.getId()).isEqualTo(created.getId());
        assertThat(retrieved.getContent()).isEqualTo(content);
        assertThat(retrieved.getRole()).isEqualTo(role);
        assertThat(retrieved.getCreatedAt()).isNotNull();
    }

    /**
     * **Feature: ai-chat-interface, Property 6: Whitespace-only messages are rejected**
     * **Validates: Requirements 2.2**
     * 
     * For any string composed entirely of whitespace characters (spaces, tabs, newlines),
     * attempting to send it as a message SHALL be rejected and the conversation state 
     * SHALL remain unchanged.
     */
    @Property(tries = 100)
    void whitespaceOnlyMessagesAreRejected(
            @ForAll("whitespaceStrings") String whitespaceContent
    ) {
        // Arrange - create user and conversation
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
            return null;
        });
        
        String conversationId = conversationIdRef.get();
        String userId = userIdRef.get();
        
        // Get initial message count
        long initialCount = messageService.countByConversation(conversationId);
        
        // Act - attempt to create message with whitespace-only content
        CreateMessageRequest request = CreateMessageRequest.builder()
                .content(whitespaceContent)
                .build();
        
        boolean exceptionThrown = false;
        try {
            messageService.create(conversationId, userId, request, MessageRole.USER);
        } catch (IllegalArgumentException e) {
            exceptionThrown = true;
        }
        
        // Assert - message was rejected
        assertThat(exceptionThrown).isTrue();
        
        // Assert - conversation state unchanged (no new messages)
        long finalCount = messageService.countByConversation(conversationId);
        assertThat(finalCount).isEqualTo(initialCount);
    }

    @Provide
    Arbitrary<String> whitespaceStrings() {
        return Arbitraries.of(
                "",
                " ",
                "  ",
                "\t",
                "\n",
                "\r",
                "\r\n",
                "   \t   ",
                "\n\n\n",
                " \t \n \r "
        );
    }
}
