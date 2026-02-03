package com.chatai.service;

import com.chatai.config.AiConfig;
import com.chatai.dto.response.MessageResponse;
import com.chatai.entity.MessageRole;
import com.chatai.exception.AiServiceException;
import com.chatai.service.impl.AiProviderClient;
import com.chatai.service.impl.AiServiceImpl;
import net.jqwik.api.*;
import net.jqwik.api.constraints.AlphaChars;
import net.jqwik.api.constraints.IntRange;
import net.jqwik.api.constraints.StringLength;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Property-based tests for AiService
 * 
 * **Feature: ai-chat-interface, Property 7: AI errors are handled gracefully**
 */
public class AiServiceProperties {

    /**
     * **Feature: ai-chat-interface, Property 7: AI errors are handled gracefully**
     * **Validates: Requirements 2.5**
     * 
     * For any AI provider error, the system SHALL return a user-friendly error response
     * and maintain the ability to retry the request.
     */
    @Property(tries = 100)
    void aiErrorsAreHandledGracefully(
            @ForAll @AlphaChars @StringLength(min = 1, max = 100) String userMessage,
            @ForAll("errorTypes") ErrorType errorType
    ) {
        // Arrange - create AiService with a failing provider client
        AiConfig config = createTestConfig();
        FailingAiClient failingClient = new FailingAiClient(errorType);
        AiServiceImpl aiService = new AiServiceImpl(config, failingClient);
        
        List<MessageResponse> history = createSampleHistory();
        
        // Act & Assert - service throws AiServiceException with appropriate properties
        assertThatThrownBy(() -> aiService.sendMessage(userMessage, history))
                .isInstanceOf(AiServiceException.class)
                .satisfies(ex -> {
                    AiServiceException aiEx = (AiServiceException) ex;
                    // Error message should be present
                    assertThat(aiEx.getMessage()).isNotNull();
                    assertThat(aiEx.getMessage()).isNotEmpty();
                    // Error code should be set
                    assertThat(aiEx.getErrorCode()).isNotNull();
                    // Retryable flag should be set appropriately based on error type
                    assertThat(aiEx.isRetryable()).isEqualTo(errorType.isRetryable());
                });
    }

    /**
     * Property: Retryable errors trigger retry attempts
     * 
     * For any retryable AI error, the service SHALL attempt retries before failing.
     */
    @Property(tries = 50)
    void retryableErrorsTriggerRetryAttempts(
            @ForAll @AlphaChars @StringLength(min = 1, max = 50) String userMessage,
            @ForAll @IntRange(min = 1, max = 3) int maxRetries
    ) {
        // Arrange - create config with specific retry count
        AiConfig config = createTestConfig();
        config.setMaxRetries(maxRetries);
        config.setRetryDelayMs(1); // Minimal delay for testing
        
        CountingFailingClient countingClient = new CountingFailingClient(true);
        AiServiceImpl aiService = new AiServiceImpl(config, countingClient);
        
        List<MessageResponse> history = createSampleHistory();
        
        // Act - attempt to send message (will fail after retries)
        try {
            aiService.sendMessage(userMessage, history);
        } catch (AiServiceException e) {
            // Expected
        }
        
        // Assert - service attempted the expected number of retries
        // Initial attempt + maxRetries = total attempts
        assertThat(countingClient.getAttemptCount()).isEqualTo(maxRetries + 1);
    }

    /**
     * Property: Non-retryable errors fail immediately
     * 
     * For any non-retryable AI error, the service SHALL fail immediately without retrying.
     */
    @Property(tries = 50)
    void nonRetryableErrorsFailImmediately(
            @ForAll @AlphaChars @StringLength(min = 1, max = 50) String userMessage
    ) {
        // Arrange - create config with retries enabled
        AiConfig config = createTestConfig();
        config.setMaxRetries(3);
        
        CountingFailingClient countingClient = new CountingFailingClient(false); // Non-retryable
        AiServiceImpl aiService = new AiServiceImpl(config, countingClient);
        
        List<MessageResponse> history = createSampleHistory();
        
        // Act - attempt to send message
        try {
            aiService.sendMessage(userMessage, history);
        } catch (AiServiceException e) {
            // Expected
        }
        
        // Assert - service did not retry (only 1 attempt)
        assertThat(countingClient.getAttemptCount()).isEqualTo(1);
    }

    /**
     * Property: Successful response after transient failures
     * 
     * For any transient AI error that recovers, the service SHALL return the successful response.
     */
    @Property(tries = 50)
    void successfulResponseAfterTransientFailures(
            @ForAll @AlphaChars @StringLength(min = 1, max = 50) String userMessage,
            @ForAll @IntRange(min = 1, max = 2) int failuresBeforeSuccess
    ) {
        // Arrange
        AiConfig config = createTestConfig();
        config.setMaxRetries(3);
        config.setRetryDelayMs(1);
        
        String expectedResponse = "AI response for: " + userMessage;
        TransientFailingClient transientClient = new TransientFailingClient(failuresBeforeSuccess, expectedResponse);
        AiServiceImpl aiService = new AiServiceImpl(config, transientClient);
        
        List<MessageResponse> history = createSampleHistory();
        
        // Act
        String response = aiService.sendMessage(userMessage, history);
        
        // Assert - got successful response after retries
        assertThat(response).isEqualTo(expectedResponse);
        assertThat(transientClient.getAttemptCount()).isEqualTo(failuresBeforeSuccess + 1);
    }

    /**
     * Property: Service availability check works correctly
     */
    @Property(tries = 20)
    void serviceAvailabilityCheckWorks(
            @ForAll boolean configured
    ) {
        // Arrange
        AiConfig config = createTestConfig();
        ConfigurableAiClient client = new ConfigurableAiClient(configured);
        AiServiceImpl aiService = new AiServiceImpl(config, client);
        
        // Act & Assert
        assertThat(aiService.isAvailable()).isEqualTo(configured);
    }

    @Provide
    Arbitrary<ErrorType> errorTypes() {
        return Arbitraries.of(ErrorType.values());
    }

    private AiConfig createTestConfig() {
        AiConfig config = new AiConfig();
        config.setProvider("mock");
        config.setMaxRetries(3);
        config.setRetryDelayMs(1);
        config.setRetryMultiplier(1.0);
        return config;
    }

    private List<MessageResponse> createSampleHistory() {
        List<MessageResponse> history = new ArrayList<>();
        history.add(MessageResponse.builder()
                .id("msg-1")
                .content("Hello")
                .role(MessageRole.USER)
                .createdAt(LocalDateTime.now().minusMinutes(2))
                .build());
        history.add(MessageResponse.builder()
                .id("msg-2")
                .content("Hi there!")
                .role(MessageRole.ASSISTANT)
                .createdAt(LocalDateTime.now().minusMinutes(1))
                .build());
        return history;
    }

    // Test helper classes

    enum ErrorType {
        NETWORK_ERROR(true, "NETWORK_ERROR"),
        RATE_LIMIT(true, "RATE_LIMIT"),
        SERVER_ERROR(true, "SERVER_ERROR"),
        AUTH_ERROR(false, "AUTH_ERROR"),
        CONFIG_ERROR(false, "CONFIG_ERROR"),
        INVALID_INPUT(false, "INVALID_INPUT");

        private final boolean retryable;
        private final String code;

        ErrorType(boolean retryable, String code) {
            this.retryable = retryable;
            this.code = code;
        }

        public boolean isRetryable() {
            return retryable;
        }

        public String getCode() {
            return code;
        }
    }

    /**
     * Test client that always fails with a specific error type
     */
    static class FailingAiClient implements AiProviderClient {
        private final ErrorType errorType;

        FailingAiClient(ErrorType errorType) {
            this.errorType = errorType;
        }

        @Override
        public String chat(String userMessage, List<MessageResponse> conversationHistory) throws AiServiceException {
            throw new AiServiceException(
                    "Simulated " + errorType.name() + " error",
                    errorType.getCode(),
                    errorType.isRetryable()
            );
        }

        @Override
        public String generateTitle(String firstUserMessage, String firstAiResponse) throws AiServiceException {
            throw new AiServiceException(
                    "Simulated " + errorType.name() + " error",
                    errorType.getCode(),
                    errorType.isRetryable()
            );
        }

        @Override
        public boolean isConfigured() {
            return true;
        }
    }

    /**
     * Test client that counts attempts and always fails
     */
    static class CountingFailingClient implements AiProviderClient {
        private final AtomicInteger attemptCount = new AtomicInteger(0);
        private final boolean retryable;

        CountingFailingClient(boolean retryable) {
            this.retryable = retryable;
        }

        @Override
        public String chat(String userMessage, List<MessageResponse> conversationHistory) throws AiServiceException {
            attemptCount.incrementAndGet();
            throw new AiServiceException("Simulated error", "TEST_ERROR", retryable);
        }

        @Override
        public String generateTitle(String firstUserMessage, String firstAiResponse) throws AiServiceException {
            attemptCount.incrementAndGet();
            throw new AiServiceException("Simulated error", "TEST_ERROR", retryable);
        }

        @Override
        public boolean isConfigured() {
            return true;
        }

        public int getAttemptCount() {
            return attemptCount.get();
        }
    }

    /**
     * Test client that fails a specified number of times before succeeding
     */
    static class TransientFailingClient implements AiProviderClient {
        private final AtomicInteger attemptCount = new AtomicInteger(0);
        private final int failuresBeforeSuccess;
        private final String successResponse;

        TransientFailingClient(int failuresBeforeSuccess, String successResponse) {
            this.failuresBeforeSuccess = failuresBeforeSuccess;
            this.successResponse = successResponse;
        }

        @Override
        public String chat(String userMessage, List<MessageResponse> conversationHistory) throws AiServiceException {
            int attempt = attemptCount.incrementAndGet();
            if (attempt <= failuresBeforeSuccess) {
                throw new AiServiceException("Transient error", "TRANSIENT_ERROR", true);
            }
            return successResponse;
        }

        @Override
        public String generateTitle(String firstUserMessage, String firstAiResponse) throws AiServiceException {
            return "Generated Title";
        }

        @Override
        public boolean isConfigured() {
            return true;
        }

        public int getAttemptCount() {
            return attemptCount.get();
        }
    }

    /**
     * Test client with configurable availability
     */
    static class ConfigurableAiClient implements AiProviderClient {
        private final boolean configured;

        ConfigurableAiClient(boolean configured) {
            this.configured = configured;
        }

        @Override
        public String chat(String userMessage, List<MessageResponse> conversationHistory) throws AiServiceException {
            return "Response";
        }

        @Override
        public String generateTitle(String firstUserMessage, String firstAiResponse) throws AiServiceException {
            return "Title";
        }

        @Override
        public boolean isConfigured() {
            return configured;
        }
    }
}
