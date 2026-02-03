package com.chatai.service.impl;

import com.chatai.dto.response.MessageResponse;
import com.chatai.exception.AiServiceException;

import java.util.List;

/**
 * Interface for AI provider clients.
 * Each AI provider (OpenAI, Claude, etc.) implements this interface.
 */
public interface AiProviderClient {
    
    /**
     * Sends a chat message to the AI provider.
     * 
     * @param userMessage The user's message
     * @param conversationHistory Previous messages for context
     * @return The AI's response
     * @throws AiServiceException if the request fails
     */
    String chat(String userMessage, List<MessageResponse> conversationHistory) throws AiServiceException;
    
    /**
     * Generates a title for a conversation.
     * 
     * @param firstUserMessage The first user message
     * @param firstAiResponse The first AI response
     * @return A generated title
     * @throws AiServiceException if the request fails
     */
    String generateTitle(String firstUserMessage, String firstAiResponse) throws AiServiceException;
    
    /**
     * Checks if the client is properly configured.
     * 
     * @return true if configured, false otherwise
     */
    boolean isConfigured();
}
