package com.chatai.security;

import org.springframework.stereotype.Service;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Service to manage blacklisted JWT tokens.
 * In production, this should be backed by Redis or a database
 * for persistence across server restarts and horizontal scaling.
 */
@Service
public class TokenBlacklistService {

    private final Set<String> blacklistedTokens = ConcurrentHashMap.newKeySet();

    /**
     * Add a token to the blacklist (used during logout).
     * @param token The JWT token to blacklist
     */
    public void blacklist(String token) {
        if (token != null && !token.isEmpty()) {
            blacklistedTokens.add(token);
        }
    }

    /**
     * Check if a token is blacklisted.
     * @param token The JWT token to check
     * @return true if the token is blacklisted, false otherwise
     */
    public boolean isBlacklisted(String token) {
        return token != null && blacklistedTokens.contains(token);
    }

    /**
     * Remove a token from the blacklist (for cleanup).
     * @param token The JWT token to remove
     */
    public void remove(String token) {
        blacklistedTokens.remove(token);
    }

    /**
     * Clear all blacklisted tokens (for testing purposes).
     */
    public void clear() {
        blacklistedTokens.clear();
    }
}
