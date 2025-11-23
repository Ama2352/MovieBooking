package com.api.moviebooking.models.dtos;

import java.util.UUID;

import com.api.moviebooking.models.enums.LockOwnerType;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SessionContext {

    /**
     * Unique identifier for the lock owner
     * - For authenticated users: User.id from JWT
     * - For guests: temporary sessionId from X-Session-Id header
     */
    private String lockOwnerId;

    /**
     * Type of lock owner (USER or GUEST_SESSION)
     */
    private LockOwnerType lockOwnerType;

    /**
     * Optional: Actual User ID if authenticated
     * Null for guest sessions until checkout
     */
    private UUID userId;

    /**
     * Check if this session belongs to an authenticated user
     */
    public boolean isAuthenticated() {
        return lockOwnerType == LockOwnerType.USER && userId != null;
    }

    /**
     * Check if this session belongs to a guest
     */
    public boolean isGuest() {
        return lockOwnerType == LockOwnerType.GUEST_SESSION;
    }

    /**
     * Create context for authenticated user
     */
    public static SessionContext forUser(UUID userId) {
        return SessionContext.builder()
                .lockOwnerId(userId.toString())
                .lockOwnerType(LockOwnerType.USER)
                .userId(userId)
                .build();
    }

    /**
     * Create context for guest session
     */
    public static SessionContext forGuest(String sessionId) {
        return SessionContext.builder()
                .lockOwnerId(sessionId)
                .lockOwnerType(LockOwnerType.GUEST_SESSION)
                .userId(null)
                .build();
    }
}
