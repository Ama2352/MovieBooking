package com.api.moviebooking.models.enums;

/**
 * Enum representing the type of entity that owns a seat lock
 * Used to distinguish between authenticated users and guest sessions
 */
public enum LockOwnerType {
    /**
     * Lock owned by an authenticated user with a database record
     * lockOwnerId = User.id (UUID)
     */
    USER,

    /**
     * Lock owned by a guest session (no database record yet)
     * lockOwnerId = temporary sessionId (UUID)
     */
    GUEST_SESSION
}
