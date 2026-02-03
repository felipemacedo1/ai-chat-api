package com.chatai.service.impl;

import com.chatai.config.AiConfig;
import com.chatai.dto.response.MessageResponse;
import com.chatai.exception.AiServiceException;
import lombok.extern.slf4j.Slf4j;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * Anthropic Claude API client implementation.
 * Supports Claude 3 models (Opus, Sonnet, Haiku).
 */
@Slf4j
public class ClaudeClient implements AiProviderClient {
    
    private static final String DEFAULT_BASE_URL = "https://api.anthropic.com/v1";
    private static final String API_VERSION = "2023-06-01";
    
    private final AiConfig config;
    private final String baseUrl;
    
    public ClaudeClient(AiConfig config) {
        this.config = config;
        this.baseUrl = config.getBaseUrl() != null ? config.getBaseUrl() : DEFAULT_BASE_URL;
    }
    
    @Override
    public String chat(String userMessage, List<MessageResponse> conversationHistory) throws AiServiceException {
        if (!isConfigured()) {
            throw new AiServiceException("Claude API key not configured", "CONFIG_ERROR", false);
        }
        
        try {
            String requestBody = buildChatRequest(userMessage, conversationHistory);
            String response = makeRequest("/messages", requestBody);
            return extractContent(response);
        } catch (AiServiceException e) {
            throw e;
        } catch (Exception e) {
            log.error("Claude API error", e);
            throw new AiServiceException("Failed to communicate with Claude: " + e.getMessage(), e, true);
        }
    }
    
    @Override
    public String generateTitle(String firstUserMessage, String firstAiResponse) throws AiServiceException {
        String prompt = "Generate a short, concise title (max 6 words) for a conversation that starts with:\n" +
                       "User: " + truncate(firstUserMessage, 200) + "\n" +
                       "Assistant: " + truncate(firstAiResponse, 200) + "\n\n" +
                       "Respond with only the title, no quotes or extra text.";
        
        try {
            String requestBody = buildSimpleRequest(prompt);
            String response = makeRequest("/messages", requestBody);
            return extractContent(response).trim();
        } catch (Exception e) {
            log.warn("Failed to generate title, using fallback", e);
            // Fallback to simple title generation
            String[] words = firstUserMessage.split("\\s+");
            int wordCount = Math.min(words.length, 5);
            StringBuilder title = new StringBuilder();
            for (int i = 0; i < wordCount; i++) {
                if (i > 0) title.append(" ");
                title.append(words[i]);
            }
            return title.toString();
        }
    }
    
    @Override
    public boolean isConfigured() {
        return config.getApiKey() != null && !config.getApiKey().trim().isEmpty();
    }
    
    private String buildChatRequest(String userMessage, List<MessageResponse> history) {
        StringBuilder messages = new StringBuilder();
        messages.append("[");
        
        boolean first = true;
        
        // Add conversation history
        if (history != null) {
            for (MessageResponse msg : history) {
                String role = msg.getRole().name().toLowerCase();
                if ("assistant".equals(role) || "user".equals(role)) {
                    if (!first) messages.append(",");
                    messages.append("{\"role\":\"").append(role)
                           .append("\",\"content\":\"").append(escapeJson(msg.getContent())).append("\"}");
                    first = false;
                }
            }
        }
        
        // Add current user message
        if (!first) messages.append(",");
        messages.append("{\"role\":\"user\",\"content\":\"").append(escapeJson(userMessage)).append("\"}");
        messages.append("]");
        
        String model = config.getModel();
        if (model == null || model.startsWith("gpt")) {
            model = "claude-3-sonnet-20240229"; // Default Claude model
        }
        
        return String.format(
            "{\"model\":\"%s\",\"messages\":%s,\"max_tokens\":%d}",
            model,
            messages.toString(),
            config.getMaxTokens()
        );
    }
    
    private String buildSimpleRequest(String prompt) {
        String model = config.getModel();
        if (model == null || model.startsWith("gpt")) {
            model = "claude-3-sonnet-20240229";
        }
        
        return String.format(
            "{\"model\":\"%s\",\"messages\":[{\"role\":\"user\",\"content\":\"%s\"}],\"max_tokens\":50}",
            model,
            escapeJson(prompt)
        );
    }
    
    private String makeRequest(String endpoint, String requestBody) throws AiServiceException {
        HttpURLConnection connection = null;
        try {
            URL url = new URL(baseUrl + endpoint);
            connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("POST");
            connection.setRequestProperty("Content-Type", "application/json");
            connection.setRequestProperty("x-api-key", config.getApiKey());
            connection.setRequestProperty("anthropic-version", API_VERSION);
            connection.setConnectTimeout(config.getConnectionTimeout());
            connection.setReadTimeout(config.getReadTimeout());
            connection.setDoOutput(true);
            
            try (OutputStream os = connection.getOutputStream()) {
                byte[] input = requestBody.getBytes(StandardCharsets.UTF_8);
                os.write(input, 0, input.length);
            }
            
            int responseCode = connection.getResponseCode();
            
            if (responseCode == 429) {
                throw new AiServiceException("Rate limit exceeded", "RATE_LIMIT", true);
            }
            
            if (responseCode == 401) {
                throw new AiServiceException("Invalid API key", "AUTH_ERROR", false);
            }
            
            if (responseCode >= 500) {
                throw new AiServiceException("Claude server error", "SERVER_ERROR", true);
            }
            
            if (responseCode != 200) {
                String errorBody = readStream(connection.getErrorStream());
                throw new AiServiceException("Claude API error: " + errorBody, "API_ERROR", responseCode >= 500);
            }
            
            return readStream(connection.getInputStream());
            
        } catch (AiServiceException e) {
            throw e;
        } catch (java.net.SocketTimeoutException e) {
            throw new AiServiceException("Request timeout", e, true);
        } catch (IOException e) {
            throw new AiServiceException("Network error: " + e.getMessage(), e, true);
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }
    
    private String readStream(InputStream stream) throws IOException {
        if (stream == null) return "";
        
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8))) {
            StringBuilder response = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                response.append(line);
            }
            return response.toString();
        }
    }
    
    private String extractContent(String jsonResponse) throws AiServiceException {
        // Claude returns content in a different format: {"content":[{"type":"text","text":"..."}]}
        try {
            int textStart = jsonResponse.indexOf("\"text\":");
            if (textStart == -1) {
                // Fallback to content field
                int contentStart = jsonResponse.indexOf("\"content\":");
                if (contentStart == -1) {
                    throw new AiServiceException("Invalid response format", "PARSE_ERROR", false);
                }
                textStart = jsonResponse.indexOf("\"text\":", contentStart);
                if (textStart == -1) {
                    throw new AiServiceException("Invalid response format", "PARSE_ERROR", false);
                }
            }
            
            int valueStart = jsonResponse.indexOf("\"", textStart + 7) + 1;
            int valueEnd = findClosingQuote(jsonResponse, valueStart);
            
            if (valueEnd == -1) {
                throw new AiServiceException("Invalid response format", "PARSE_ERROR", false);
            }
            
            return unescapeJson(jsonResponse.substring(valueStart, valueEnd));
        } catch (AiServiceException e) {
            throw e;
        } catch (Exception e) {
            throw new AiServiceException("Failed to parse response: " + e.getMessage(), e, false);
        }
    }
    
    private int findClosingQuote(String str, int start) {
        for (int i = start; i < str.length(); i++) {
            char c = str.charAt(i);
            if (c == '\\') {
                i++; // Skip escaped character
            } else if (c == '"') {
                return i;
            }
        }
        return -1;
    }
    
    private String escapeJson(String text) {
        if (text == null) return "";
        return text
            .replace("\\", "\\\\")
            .replace("\"", "\\\"")
            .replace("\n", "\\n")
            .replace("\r", "\\r")
            .replace("\t", "\\t");
    }
    
    private String unescapeJson(String text) {
        if (text == null) return "";
        return text
            .replace("\\n", "\n")
            .replace("\\r", "\r")
            .replace("\\t", "\t")
            .replace("\\\"", "\"")
            .replace("\\\\", "\\");
    }
    
    private String truncate(String text, int maxLength) {
        if (text == null) return "";
        if (text.length() <= maxLength) return text;
        return text.substring(0, maxLength) + "...";
    }
}
