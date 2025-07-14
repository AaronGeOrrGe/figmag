package com.figmine.backend.security;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Service for managing blacklisted JWT tokens.
 * Uses an in-memory ConcurrentHashMap for thread-safe operations.
 * Automatically cleans up expired tokens on a schedule.
 */
@Slf4j
@Service
public class TokenBlacklistService {
    // In-memory store for blacklisted tokens with their expiration time (seconds since epoch)
    private final Map<String, Long> blacklistedTokens = new ConcurrentHashMap<>();
    
    // Cleanup task lock to prevent multiple cleanup tasks from running simultaneously
    private final Object cleanupLock = new Object();

    @PostConstruct
    public void init() {
        log.info("TokenBlacklistService initialized");
    }

    /**
     * Add a token to the blacklist until it expires
     * @param token The JWT token to blacklist
     * @param expiresAt When the token expires (in seconds since epoch)
     */
    public void blacklistToken(String token, long expiresAt) {
        if (token == null || token.trim().isEmpty()) {
            log.warn("Attempted to blacklist null or empty token");
            return;
        }
        
        long now = Instant.now().getEpochSecond();
        if (expiresAt <= now) {
            log.debug("Token already expired, not blacklisting");
            return;
        }
        
        blacklistedTokens.put(token, expiresAt);
        log.debug("Token blacklisted until: {}", Instant.ofEpochSecond(expiresAt));
    }

    /**
     * Check if a token is blacklisted
     * @param token The JWT token to check
     * @return true if the token is blacklisted and not expired, false otherwise
     */
    public boolean isBlacklisted(String token) {
        if (token == null || token.trim().isEmpty()) {
            return false;
        }
        
        Long expiration = blacklistedTokens.get(token);
        if (expiration == null) {
            return false;
        }
        
        long now = Instant.now().getEpochSecond();
        if (expiration < now) {
            // Token expired, remove from blacklist
            blacklistedTokens.remove(token);
            log.debug("Expired token removed from blacklist");
            return false;
        }
        
        log.debug("Token found in blacklist, expires at: {}", Instant.ofEpochSecond(expiration));
        return true;
    }

    /**
     * Remove expired tokens from the blacklist
     * This runs periodically to clean up the blacklist
     */
    @Scheduled(fixedRate = 3_600_000) // Run every hour
    public void cleanupExpiredTokens() {
        synchronized (cleanupLock) {
            try {
                long now = Instant.now().getEpochSecond();
                int initialSize = blacklistedTokens.size();
                
                blacklistedTokens.entrySet().removeIf(entry -> {
                    boolean expired = entry.getValue() < now;
                    if (expired) {
                        log.trace("Removing expired token from blacklist");
                    }
                    return expired;
                });
                
                int removed = initialSize - blacklistedTokens.size();
                if (removed > 0) {
                    log.info("Cleaned up {} expired tokens from blacklist", removed);
                }
            } catch (Exception e) {
                log.error("Error during token blacklist cleanup", e);
            }
        }
    }

    /**
     * Manually remove a token from the blacklist
     * @param token The token to remove
     */
    public void removeFromBlacklist(String token) {
        if (token != null) {
            boolean removed = blacklistedTokens.remove(token) != null;
            if (removed) {
                log.debug("Token manually removed from blacklist");
            }
        }
    }

    /**
     * Get the number of blacklisted tokens
     * @return The current count of blacklisted tokens
     */
    public int getBlacklistSize() {
        return blacklistedTokens.size();
    }
}
