package com.chatai.service.impl;

import com.chatai.dto.response.MessageResponse;
import com.chatai.exception.AiServiceException;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

/**
 * Mock AI client for development and testing.
 * Returns predefined responses without making actual API calls.
 */
@Slf4j
public class MockAiClient implements AiProviderClient {
    
    @Override
    public String chat(String userMessage, List<MessageResponse> conversationHistory) throws AiServiceException {
        log.debug("Mock AI processing message: {}", userMessage);
        
        // Simulate some processing time
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        // Generate a mock response based on the user message
        String response = generateMockResponse(userMessage);
        log.debug("Mock AI response: {}", response);
        
        return response;
    }
    
    @Override
    public String generateTitle(String firstUserMessage, String firstAiResponse) throws AiServiceException {
        log.debug("Mock AI generating title for: {}", firstUserMessage);
        
        // Generate a simple title from the first few words
        String[] words = firstUserMessage.split("\\s+");
        int wordCount = Math.min(words.length, 5);
        StringBuilder title = new StringBuilder();
        
        for (int i = 0; i < wordCount; i++) {
            if (i > 0) title.append(" ");
            title.append(words[i]);
        }
        
        if (words.length > 5) {
            title.append("...");
        }
        
        return title.toString();
    }
    
    @Override
    public boolean isConfigured() {
        return true; // Mock client is always configured
    }
    
    private String generateMockResponse(String userMessage) {
        String lowerMessage = userMessage.toLowerCase();
        
        if (lowerMessage.contains("hello") || lowerMessage.contains("hi")) {
            return "Hello! I'm a mock AI assistant. How can I help you today?";
        }
        
        if (lowerMessage.contains("help")) {
            return "I'm here to help! This is a mock response for development purposes. " +
                   "In production, this would be replaced with actual AI responses.";
        }
        
        if (lowerMessage.contains("code") || lowerMessage.contains("programming")) {
            return "I can help with coding questions! Here's a simple example:\n\n" +
                   "```java\npublic class HelloWorld {\n    public static void main(String[] args) {\n" +
                   "        System.out.println(\"Hello, World!\");\n    }\n}\n```\n\n" +
                   "This is a mock response for testing purposes.";
        }
        
        return "Thank you for your message: \"" + truncate(userMessage, 50) + "\"\n\n" +
               "This is a mock AI response. In production, this would be replaced with " +
               "actual responses from an AI provider like OpenAI or Claude.";
    }
    
    private String truncate(String text, int maxLength) {
        if (text.length() <= maxLength) {
            return text;
        }
        return text.substring(0, maxLength) + "...";
    }
}
