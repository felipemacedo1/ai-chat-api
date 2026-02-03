package com.chatai.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration properties for AI provider integration.
 */
@Configuration
@ConfigurationProperties(prefix = "ai")
@Data
public class AiConfig {
    
    /**
     * The AI provider to use (openai, claude, mock)
     */
    private String provider = "mock";
    
    /**
     * API key for the AI provider
     */
    private String apiKey;
    
    /**
     * API base URL (optional, for custom endpoints)
     */
    private String baseUrl;
    
    /**
     * Model to use (e.g., gpt-4, claude-3-sonnet)
     */
    private String model = "gpt-3.5-turbo";
    
    /**
     * Maximum tokens for AI response
     */
    private int maxTokens = 2048;
    
    /**
     * Temperature for response generation (0.0 - 1.0)
     */
    private double temperature = 0.7;
    
    /**
     * Connection timeout in milliseconds
     */
    private int connectionTimeout = 30000;
    
    /**
     * Read timeout in milliseconds
     */
    private int readTimeout = 60000;
    
    /**
     * Maximum number of retry attempts
     */
    private int maxRetries = 3;
    
    /**
     * Initial delay between retries in milliseconds
     */
    private long retryDelayMs = 1000;
    
    /**
     * Multiplier for exponential backoff
     */
    private double retryMultiplier = 2.0;
}
