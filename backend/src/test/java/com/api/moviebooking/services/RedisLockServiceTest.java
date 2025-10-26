package com.api.moviebooking.services;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

@ExtendWith(MockitoExtension.class)
class RedisLockServiceTest {

    @Mock
    private RedisTemplate<String, Object> redisTemplate;

    @Mock
    private ValueOperations<String, Object> valueOperations;

    @InjectMocks
    private RedisLockService redisLockService;

    private String lockKey;
    private String lockValue;
    private long ttlSeconds;

    @BeforeEach
    void setUp() {
        lockKey = "lock:test:key";
        lockValue = UUID.randomUUID().toString();
        ttlSeconds = 600L;
    }

    // ==================== acquireLock Tests ====================

    @Test
    void testAcquireLock_Success() {
        // Arrange
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.setIfAbsent(eq(lockKey), eq(lockValue), eq(Duration.ofSeconds(ttlSeconds))))
                .thenReturn(true);

        // Act
        boolean result = redisLockService.acquireLock(lockKey, lockValue, ttlSeconds);

        // Assert
        assertTrue(result);
        verify(valueOperations).setIfAbsent(eq(lockKey), eq(lockValue), eq(Duration.ofSeconds(ttlSeconds)));
    }

    @Test
    void testAcquireLock_Failure_AlreadyLocked() {
        // Arrange
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.setIfAbsent(eq(lockKey), eq(lockValue), eq(Duration.ofSeconds(ttlSeconds))))
                .thenReturn(false);

        // Act
        boolean result = redisLockService.acquireLock(lockKey, lockValue, ttlSeconds);

        // Assert
        assertFalse(result);
        verify(valueOperations).setIfAbsent(eq(lockKey), eq(lockValue), eq(Duration.ofSeconds(ttlSeconds)));
    }

    @Test
    void testAcquireLock_RedisException() {
        // Arrange
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.setIfAbsent(any(), any(), any()))
                .thenThrow(new RuntimeException("Redis connection error"));

        // Act
        boolean result = redisLockService.acquireLock(lockKey, lockValue, ttlSeconds);

        // Assert
        assertFalse(result);
    }

    @Test
    void testAcquireLock_NullResponse() {
        // Arrange
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.setIfAbsent(any(), any(), any())).thenReturn(null);

        // Act
        boolean result = redisLockService.acquireLock(lockKey, lockValue, ttlSeconds);

        // Assert
        assertFalse(result);
    }

    // ==================== releaseLock Tests ====================

    @Test
    void testReleaseLock_Success() {
        // Arrange
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(lockKey)).thenReturn(lockValue);
        when(redisTemplate.delete(lockKey)).thenReturn(true);

        // Act
        boolean result = redisLockService.releaseLock(lockKey, lockValue);

        // Assert
        assertTrue(result);
        verify(valueOperations).get(lockKey);
        verify(redisTemplate).delete(lockKey);
    }

    @Test
    void testReleaseLock_NotOwned() {
        // Arrange
        String differentLockValue = UUID.randomUUID().toString();
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(lockKey)).thenReturn(differentLockValue);

        // Act
        boolean result = redisLockService.releaseLock(lockKey, lockValue);

        // Assert
        assertFalse(result);
        verify(valueOperations).get(lockKey);
        verify(redisTemplate, never()).delete(anyString());
    }

    @Test
    void testReleaseLock_LockDoesNotExist() {
        // Arrange
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(lockKey)).thenReturn(null);

        // Act
        boolean result = redisLockService.releaseLock(lockKey, lockValue);

        // Assert
        assertFalse(result);
        verify(valueOperations).get(lockKey);
        verify(redisTemplate, never()).delete(anyString());
    }

    @Test
    void testReleaseLock_RedisException() {
        // Arrange
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(lockKey)).thenThrow(new RuntimeException("Redis connection error"));

        // Act
        boolean result = redisLockService.releaseLock(lockKey, lockValue);

        // Assert
        assertFalse(result);
    }

    @Test
    void testReleaseLock_DeleteFails() {
        // Arrange
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(lockKey)).thenReturn(lockValue);
        when(redisTemplate.delete(lockKey)).thenReturn(false);

        // Act
        boolean result = redisLockService.releaseLock(lockKey, lockValue);

        // Assert
        assertFalse(result);
        verify(redisTemplate).delete(lockKey);
    }

    // ==================== isLocked Tests ====================

    @Test
    void testIsLocked_Exists() {
        // Arrange
        when(redisTemplate.hasKey(lockKey)).thenReturn(true);

        // Act
        boolean result = redisLockService.isLocked(lockKey);

        // Assert
        assertTrue(result);
        verify(redisTemplate).hasKey(lockKey);
    }

    @Test
    void testIsLocked_DoesNotExist() {
        // Arrange
        when(redisTemplate.hasKey(lockKey)).thenReturn(false);

        // Act
        boolean result = redisLockService.isLocked(lockKey);

        // Assert
        assertFalse(result);
        verify(redisTemplate).hasKey(lockKey);
    }

    @Test
    void testIsLocked_RedisException() {
        // Arrange
        when(redisTemplate.hasKey(lockKey)).thenThrow(new RuntimeException("Redis connection error"));

        // Act
        boolean result = redisLockService.isLocked(lockKey);

        // Assert
        assertFalse(result);
    }

    // ==================== getLockTTL Tests ====================

    @Test
    void testGetLockTTL_Success() {
        // Arrange
        long expectedTTL = 300L;
        when(redisTemplate.getExpire(lockKey, TimeUnit.SECONDS)).thenReturn(expectedTTL);

        // Act
        Long result = redisLockService.getLockTTL(lockKey);

        // Assert
        assertEquals(expectedTTL, result);
        verify(redisTemplate).getExpire(lockKey, TimeUnit.SECONDS);
    }

    @Test
    void testGetLockTTL_RedisException() {
        // Arrange
        when(redisTemplate.getExpire(lockKey, TimeUnit.SECONDS))
                .thenThrow(new RuntimeException("Redis connection error"));

        // Act
        Long result = redisLockService.getLockTTL(lockKey);

        // Assert
        assertEquals(-1L, result);
    }

    // ==================== extendLock Tests ====================

    @Test
    void testExtendLock_Success() {
        // Arrange
        long additionalSeconds = 300L;
        long currentTTL = 200L;
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(lockKey)).thenReturn(lockValue);
        when(redisTemplate.getExpire(lockKey, TimeUnit.SECONDS)).thenReturn(currentTTL);
        when(redisTemplate.expire(eq(lockKey), eq(Duration.ofSeconds(currentTTL + additionalSeconds))))
                .thenReturn(true);

        // Act
        boolean result = redisLockService.extendLock(lockKey, lockValue, additionalSeconds);

        // Assert
        assertTrue(result);
        verify(valueOperations).get(lockKey);
        verify(redisTemplate).getExpire(lockKey, TimeUnit.SECONDS);
        verify(redisTemplate).expire(eq(lockKey), eq(Duration.ofSeconds(currentTTL + additionalSeconds)));
    }

    @Test
    void testExtendLock_NotOwned() {
        // Arrange
        String differentLockValue = UUID.randomUUID().toString();
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(lockKey)).thenReturn(differentLockValue);

        // Act
        boolean result = redisLockService.extendLock(lockKey, lockValue, 300L);

        // Assert
        assertFalse(result);
        verify(valueOperations).get(lockKey);
        verify(redisTemplate, never()).expire(any(), any());
    }

    @Test
    void testExtendLock_NegativeTTL() {
        // Arrange
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(lockKey)).thenReturn(lockValue);
        when(redisTemplate.getExpire(lockKey, TimeUnit.SECONDS)).thenReturn(-1L);

        // Act
        boolean result = redisLockService.extendLock(lockKey, lockValue, 300L);

        // Assert
        assertFalse(result);
        verify(redisTemplate, never()).expire(any(), any());
    }

    @Test
    void testExtendLock_RedisException() {
        // Arrange
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(lockKey)).thenThrow(new RuntimeException("Redis connection error"));

        // Act
        boolean result = redisLockService.extendLock(lockKey, lockValue, 300L);

        // Assert
        assertFalse(result);
    }

    // ==================== Key Generation Tests ====================

    @Test
    void testGenerateSeatLockKey() {
        // Arrange
        UUID showtimeId = UUID.randomUUID();
        UUID seatId = UUID.randomUUID();

        // Act
        String key = redisLockService.generateSeatLockKey(showtimeId, seatId);

        // Assert
        assertEquals("lock:seat:" + showtimeId + ":" + seatId, key);
    }

    @Test
    void testGenerateUserSessionKey() {
        // Arrange
        UUID userId = UUID.randomUUID();
        UUID showtimeId = UUID.randomUUID();

        // Act
        String key = redisLockService.generateUserSessionKey(userId, showtimeId);

        // Assert
        assertEquals("lock:user:" + userId + ":showtime:" + showtimeId, key);
    }

    @Test
    void testGenerateShowtimeLockKey() {
        // Arrange
        UUID showtimeId = UUID.randomUUID();

        // Act
        String key = redisLockService.generateShowtimeLockKey(showtimeId);

        // Assert
        assertEquals("lock:showtime:" + showtimeId, key);
    }

    // ==================== acquireMultipleSeatsLock Tests ====================

    @Test
    void testAcquireMultipleSeatsLock_Success() {
        // Arrange
        UUID showtimeId = UUID.randomUUID();
        UUID seatId1 = UUID.randomUUID();
        UUID seatId2 = UUID.randomUUID();
        List<UUID> seatIds = Arrays.asList(seatId1, seatId2);
        String lockToken = UUID.randomUUID().toString();

        String seatKey1 = "lock:seat:" + showtimeId + ":" + seatId1;
        String seatKey2 = "lock:seat:" + showtimeId + ":" + seatId2;

        // All seats are not locked
        when(redisTemplate.hasKey(seatKey1)).thenReturn(false);
        when(redisTemplate.hasKey(seatKey2)).thenReturn(false);

        // Successfully acquire both locks
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.setIfAbsent(eq(seatKey1), eq(lockToken), any())).thenReturn(true);
        when(valueOperations.setIfAbsent(eq(seatKey2), eq(lockToken), any())).thenReturn(true);

        // Act
        boolean result = redisLockService.acquireMultipleSeatsLock(showtimeId, seatIds, lockToken, ttlSeconds);

        // Assert
        assertTrue(result);
        verify(redisTemplate).hasKey(seatKey1);
        verify(redisTemplate).hasKey(seatKey2);
        verify(valueOperations).setIfAbsent(eq(seatKey1), eq(lockToken), any());
        verify(valueOperations).setIfAbsent(eq(seatKey2), eq(lockToken), any());
    }

    @Test
    void testAcquireMultipleSeatsLock_OneSeatLocked() {
        // Arrange
        UUID showtimeId = UUID.randomUUID();
        UUID seatId1 = UUID.randomUUID();
        UUID seatId2 = UUID.randomUUID();
        List<UUID> seatIds = Arrays.asList(seatId1, seatId2);
        String lockToken = UUID.randomUUID().toString();

        String seatKey1 = "lock:seat:" + showtimeId + ":" + seatId1;
        String seatKey2 = "lock:seat:" + showtimeId + ":" + seatId2;

        // First seat is available, second is locked
        when(redisTemplate.hasKey(seatKey1)).thenReturn(false);
        when(redisTemplate.hasKey(seatKey2)).thenReturn(true);

        // Act
        boolean result = redisLockService.acquireMultipleSeatsLock(showtimeId, seatIds, lockToken, ttlSeconds);

        // Assert
        assertFalse(result);
        verify(redisTemplate).hasKey(seatKey1);
        verify(redisTemplate).hasKey(seatKey2);
        // Should not attempt to acquire any locks
        verify(redisTemplate, never()).opsForValue();
    }

    @Test
    void testAcquireMultipleSeatsLock_PartialFailure_Rollback() {
        // Arrange
        UUID showtimeId = UUID.randomUUID();
        UUID seatId1 = UUID.randomUUID();
        UUID seatId2 = UUID.randomUUID();
        List<UUID> seatIds = Arrays.asList(seatId1, seatId2);
        String lockToken = UUID.randomUUID().toString();

        String seatKey1 = "lock:seat:" + showtimeId + ":" + seatId1;
        String seatKey2 = "lock:seat:" + showtimeId + ":" + seatId2;

        // All seats appear available initially
        when(redisTemplate.hasKey(seatKey1)).thenReturn(false);
        when(redisTemplate.hasKey(seatKey2)).thenReturn(false);

        // First lock succeeds, second fails (race condition)
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.setIfAbsent(eq(seatKey1), eq(lockToken), any())).thenReturn(true);
        when(valueOperations.setIfAbsent(eq(seatKey2), eq(lockToken), any())).thenReturn(false);

        // For rollback
        when(valueOperations.get(seatKey1)).thenReturn(lockToken);
        when(redisTemplate.delete(seatKey1)).thenReturn(true);

        // Act
        boolean result = redisLockService.acquireMultipleSeatsLock(showtimeId, seatIds, lockToken, ttlSeconds);

        // Assert
        assertFalse(result);
        // Verify rollback was called for seat 1
        verify(valueOperations).get(seatKey1);
        verify(redisTemplate).delete(seatKey1);
    }

    // ==================== releaseMultipleSeatsLock Tests ====================

    @Test
    void testReleaseMultipleSeatsLock() {
        // Arrange
        UUID showtimeId = UUID.randomUUID();
        UUID seatId1 = UUID.randomUUID();
        UUID seatId2 = UUID.randomUUID();
        List<UUID> seatIds = Arrays.asList(seatId1, seatId2);
        String lockToken = UUID.randomUUID().toString();

        String seatKey1 = "lock:seat:" + showtimeId + ":" + seatId1;
        String seatKey2 = "lock:seat:" + showtimeId + ":" + seatId2;

        // Setup for successful release
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(seatKey1)).thenReturn(lockToken);
        when(valueOperations.get(seatKey2)).thenReturn(lockToken);
        when(redisTemplate.delete(seatKey1)).thenReturn(true);
        when(redisTemplate.delete(seatKey2)).thenReturn(true);

        // Act
        redisLockService.releaseMultipleSeatsLock(showtimeId, seatIds, lockToken);

        // Assert
        verify(valueOperations).get(seatKey1);
        verify(valueOperations).get(seatKey2);
        verify(redisTemplate).delete(seatKey1);
        verify(redisTemplate).delete(seatKey2);
    }

    @Test
    void testAcquireMultipleSeatsLock_EmptyList() {
        // Arrange
        UUID showtimeId = UUID.randomUUID();
        List<UUID> seatIds = Arrays.asList();
        String lockToken = UUID.randomUUID().toString();

        // Act
        boolean result = redisLockService.acquireMultipleSeatsLock(showtimeId, seatIds, lockToken, ttlSeconds);

        // Assert
        assertTrue(result); // Should succeed with empty list
        verify(redisTemplate, never()).hasKey(any());
        verify(redisTemplate, never()).opsForValue();
    }
}
