package com.chatai.service.impl;

import com.chatai.config.AiConfig;
import com.chatai.dto.response.MessageResponse;
import com.chatai.exception.AiServiceException;
import com.chatai.service.AiService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Implementation of AiService that integrates with AI providers (OpenAI/Claude).
 * Includes retry logic with exponential backoff for transient failures.
 */
@Service
@Slf4j
public class AiServiceImpl implements AiService {
    
    private final AiConfig aiConfig;
    private final AiProviderClient aiProviderClient;
    
    @Autowired
    public AiServiceImpl(AiConfig aiConfig) {
        this.aiConfig = aiConfig;
        this.aiProviderClient = createProviderClient(aiConfig);
    }
    
    // Constructor for testing with injected client
    public AiServiceImpl(AiConfig aiConfig, AiProviderClient aiProviderClient) {
        this.aiConfig = aiConfig;
        this.aiProviderClient = aiProviderClient;
    }
    
    private AiProviderClient createProviderClient(AiConfig config) {
        String provider = config.getProvider();
        if (provider == null) {
            provider = "mock";
        }
        
        switch (provider.toLowerCase()) {
            case "openai":
                return new OpenAiClient(config);
            case "claude":
                return new ClaudeClient(config);
            case "mock":
            default:
                log.info("Using mock AI provider");
                return new MockAiClient();
        }
    }
    
    @Override
    public String sendMessage(String userMessage, List<MessageResponse> conversationHistory) throws AiServiceException {
        if (userMessage == null || userMessage.trim().isEmpty()) {
            throw new AiServiceException("User message cannot be empty", "INVALID_INPUT", false);
        }
        
        return executeWithRetry(() -> aiProviderClient.chat(userMessage, conversationHistory));
    }
    
    @Override
    public String generateTitle(String firstUserMessage, String firstAiResponse) throws AiServiceException {
        if (firstUserMessage == null || firstUserMessage.trim().isEmpty()) {
            throw new AiServiceException("First user message cannot be empty", "INVALID_INPUT", false);
        }
        
        return executeWithRetry(() -> aiProviderClient.generateTitle(firstUserMessage, firstAiResponse));
    }
    
    @Override
    public boolean isAvailable() {
        return aiProviderClient != null && aiProviderClient.isConfigured();
    }
    
    /**
     * Executes an operation with retry logic using exponential backoff.
     */
    private <T> T executeWithRetry(RetryableOperation<T> operation) throws AiServiceException {
        int maxRetries = aiConfig.getMaxRetries();
        long delayMs = aiConfig.getRetryDelayMs();
        double multiplier = aiConfig.getRetryMultiplier();
        
        AiServiceException lastException = null;
        
        for (int attempt = 0; attempt <= maxRetries; attempt++) {
            try {
                return operation.execute();
            } catch (AiServiceException e) {
                lastException = e;
                
                if (!e.isRetryable()) {
                    log.error("Non-retryable AI error: {}", e.getMessage());
                    throw e;
                }
                
                if (attempt < maxRetries) {
                    log.warn("AI request failed (attempt {}/{}): {}. Retrying in {}ms...", 
                            attempt + 1, maxRetries + 1, e.getMessage(), delayMs);
                    
                    try {
                        Thread.sleep(delayMs);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        throw new AiServiceException("Operation interrupted", ie, false);
                    }
                    
                    delayMs = (long) (delayMs * multiplier);
                }
            }
        }
        
        log.error("AI request failed after {} attempts", maxRetries + 1);
        throw lastException != null ? lastException : 
                new AiServiceException("AI service unavailable after retries", true);
    }
    
    @FunctionalInterface
    interface RetryableOperation<T> {
        T execute() throws AiServiceException;
    }
}
