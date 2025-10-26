package com.api.moviebooking.services;

import java.time.Duration;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Service for managing distributed locks using Redis
 * Provides atomic operations for seat locking mechanism
 * 
 * How Distributed Locking Works:
 * 1. SET NX (Set if Not eXists) - Atomic operation to acquire lock
 * 2. TTL (Time To Live) - Auto-expire locks after timeout
 * 3. Unique token per lock - Prevent accidental release by another user
 * 4. Single Redis instance coordinates all servers
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class RedisLockService {

    private final RedisTemplate<String, Object> redisTemplate;

    // Lock prefixes for different types
    private static final String SEAT_LOCK_PREFIX = "lock:seat:";
    private static final String USER_LOCK_PREFIX = "lock:user:";
    private static final String SHOWTIME_LOCK_PREFIX = "lock:showtime:";

    /**
     * Attempt to acquire a distributed lock
     * 
     * @param lockKey    Unique key for the lock
     * @param lockValue  Unique identifier (usually UUID) to identify lock owner
     * @param ttlSeconds Time to live in seconds
     * @return true if lock acquired, false if already locked
     */
    public boolean acquireLock(String lockKey, String lockValue, long ttlSeconds) {
        try {
            // SET NX EX - Atomic operation that sets key only if it doesn't exist with
            // expiration
            Boolean success = redisTemplate.opsForValue()
                    .setIfAbsent(lockKey, lockValue, Duration.ofSeconds(ttlSeconds));

            if (Boolean.TRUE.equals(success)) {
                log.debug("Lock acquired successfully: {}", lockKey);
                return true;
            }

            log.debug("Lock acquisition failed (already exists): {}", lockKey);
            return false;
        } catch (Exception e) {
            log.error("Error acquiring lock: {}", lockKey, e);
            return false;
        }
    }

    /**
     * Release a distributed lock (only if owned by the caller)
     * 
     * @param lockKey   The lock key to release
     * @param lockValue The lock owner's identifier
     * @return true if released, false if not owned or doesn't exist
     */
    public boolean releaseLock(String lockKey, String lockValue) {
        try {
            // Get current lock value
            Object currentValue = redisTemplate.opsForValue().get(lockKey);

            // Only delete if we own the lock (prevents accidental release)
            if (lockValue.equals(currentValue)) {
                Boolean deleted = redisTemplate.delete(lockKey);
                if (Boolean.TRUE.equals(deleted)) {
                    log.debug("Lock released successfully: {}", lockKey);
                    return true;
                }
            }

            log.debug("Lock release failed (not owned or expired): {}", lockKey);
            return false;
        } catch (Exception e) {
            log.error("Error releasing lock: {}", lockKey, e);
            return false;
        }
    }

    /**
     * Check if a lock exists and is active
     */
    public boolean isLocked(String lockKey) {
        try {
            return Boolean.TRUE.equals(redisTemplate.hasKey(lockKey));
        } catch (Exception e) {
            log.error("Error checking lock: {}", lockKey, e);
            return false;
        }
    }

    /**
     * Get remaining TTL for a lock in seconds
     */
    public Long getLockTTL(String lockKey) {
        try {
            return redisTemplate.getExpire(lockKey, TimeUnit.SECONDS);
        } catch (Exception e) {
            log.error("Error getting lock TTL: {}", lockKey, e);
            return -1L;
        }
    }

    /**
     * Extend lock TTL (if owned by caller)
     */
    public boolean extendLock(String lockKey, String lockValue, long additionalSeconds) {
        try {
            Object currentValue = redisTemplate.opsForValue().get(lockKey);

            if (lockValue.equals(currentValue)) {
                Long currentTTL = getLockTTL(lockKey);
                if (currentTTL != null && currentTTL > 0) {
                    return Boolean.TRUE.equals(
                            redisTemplate.expire(lockKey, Duration.ofSeconds(currentTTL + additionalSeconds)));
                }
            }
            return false;
        } catch (Exception e) {
            log.error("Error extending lock: {}", lockKey, e);
            return false;
        }
    }

    /**
     * Generate lock key for a specific seat in a showtime
     */
    public String generateSeatLockKey(UUID showtimeId, UUID seatId) {
        return SEAT_LOCK_PREFIX + showtimeId + ":" + seatId;
    }

    /**
     * Generate lock key for user's booking session
     */
    public String generateUserSessionKey(UUID userId, UUID showtimeId) {
        return USER_LOCK_PREFIX + userId + ":showtime:" + showtimeId;
    }

    /**
     * Generate lock key for showtime-level operations
     */
    public String generateShowtimeLockKey(UUID showtimeId) {
        return SHOWTIME_LOCK_PREFIX + showtimeId;
    }

    /**
     * Lock multiple seats atomically for a user
     * Returns true only if ALL seats can be locked
     */
    public boolean acquireMultipleSeatsLock(UUID showtimeId, Iterable<UUID> seatIds,
            String lockToken, long ttlSeconds) {
        // First, check if any seat is already locked
        for (UUID seatId : seatIds) {
            String seatKey = generateSeatLockKey(showtimeId, seatId);
            if (isLocked(seatKey)) {
                log.warn("Seat already locked: {} for showtime: {}", seatId, showtimeId);
                return false;
            }
        }

        // If all available, lock them all
        boolean allLocked = true;
        for (UUID seatId : seatIds) {
            String seatKey = generateSeatLockKey(showtimeId, seatId);
            if (!acquireLock(seatKey, lockToken, ttlSeconds)) {
                // Rollback: release all previously locked seats
                rollbackSeatLocks(showtimeId, seatIds, lockToken);
                allLocked = false;
                break;
            }
        }

        return allLocked;
    }

    /**
     * Release multiple seat locks
     */
    public void releaseMultipleSeatsLock(UUID showtimeId, Iterable<UUID> seatIds, String lockToken) {
        for (UUID seatId : seatIds) {
            String seatKey = generateSeatLockKey(showtimeId, seatId);
            releaseLock(seatKey, lockToken);
        }
    }

    /**
     * Rollback seat locks in case of partial failure
     */
    private void rollbackSeatLocks(UUID showtimeId, Iterable<UUID> seatIds, String lockToken) {
        log.warn("Rolling back seat locks for showtime: {}", showtimeId);
        releaseMultipleSeatsLock(showtimeId, seatIds, lockToken);
    }
}
