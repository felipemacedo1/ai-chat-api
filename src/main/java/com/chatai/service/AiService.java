package com.chatai.service;

import com.chatai.dto.response.MessageResponse;
import com.chatai.exception.AiServiceException;

import java.util.List;

/**
 * Interface for AI provider integration.
 * Supports sending messages with conversation context and receiving AI responses.
 */
public interface AiService {
    
    /**
     * Sends a message to the AI provider with conversation context.
     * 
     * @param userMessage The user's message content
     * @param conversationHistory Previous messages in the conversation for context
     * @return The AI's response content
     * @throws AiServiceException if the AI provider returns an error or is unavailable
     */
    String sendMessage(String userMessage, List<MessageResponse> conversationHistory) throws AiServiceException;
    
    /**
     * Generates a title for a conversation based on its content.
     * 
     * @param firstUserMessage The first user message in the conversation
     * @param firstAiResponse The first AI response in the conversation
     * @return A generated title for the conversation
     * @throws AiServiceException if the AI provider returns an error
     */
    String generateTitle(String firstUserMessage, String firstAiResponse) throws AiServiceException;
    
    /**
     * Checks if the AI service is available and properly configured.
     * 
     * @return true if the service is available, false otherwise
     */
    boolean isAvailable();
}
