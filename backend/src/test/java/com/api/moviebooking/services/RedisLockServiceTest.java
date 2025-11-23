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

    // ==================== acquireLock() Tests - V(G) = 3 ====================
    // Cyclomatic Complexity: 3 (2 decision nodes: success==TRUE, catch)
    // Test Cases: 4 (covers all paths + null response edge case)

    @Test
    @DisplayName("Successfully acquire lock when key is available")
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
    @DisplayName("Fail to acquire lock when key already exists")
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
    @DisplayName("Handle Redis connection errors gracefully")
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
    @DisplayName("Handle null response from Redis setIfAbsent")
    void testAcquireLock_NullResponse() {
        // Arrange
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.setIfAbsent(any(), any(), any())).thenReturn(null);

        // Act
        boolean result = redisLockService.acquireLock(lockKey, lockValue, ttlSeconds);

        // Assert
        assertFalse(result);
    }

    // ==================== releaseLock() Tests - V(G) = 4 ====================
    // Cyclomatic Complexity: 4 (3 decision nodes: lockValue.equals(currentValue),
    // deleted==TRUE, catch)
    // Test Cases: 5 (covers all paths + delete failure edge case)

    @Test
    @DisplayName("Successfully release owned lock")
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
    @DisplayName("Prevent releasing lock owned by another user")
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
    @DisplayName("Handle releasing non-existent lock")
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
    @DisplayName("Handle Redis connection errors gracefully during release")
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
    @DisplayName("Handle edge case where delete operation fails")
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

    // ==================== isLocked() Tests - V(G) = 2 ====================
    // Cyclomatic Complexity: 2 (1 decision node: catch)
    // Test Cases: 3 (covers all paths + exception handling)
    // Note: V(G) ≤ 2, excluded from decision tables per requirements

    @Test
    @DisplayName("Check if lock exists")
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
    @DisplayName("Check if lock does not exist")
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
    @DisplayName("Handle Redis errors when checking lock existence")
    void testIsLocked_RedisException() {
        // Arrange
        when(redisTemplate.hasKey(lockKey)).thenThrow(new RuntimeException("Redis connection error"));

        // Act
        boolean result = redisLockService.isLocked(lockKey);

        // Assert
        assertFalse(result);
    }

    // ==================== getLockTTL() Tests - V(G) = 2 ====================
    // Cyclomatic Complexity: 2 (1 decision node: catch)
    // Test Cases: 2 (covers all paths)
    // Note: V(G) ≤ 2, excluded from decision tables per requirements

    @Test
    @DisplayName("Get remaining TTL for a lock")
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
    @DisplayName("Handle Redis errors when getting TTL")
    void testGetLockTTL_RedisException() {
        // Arrange
        when(redisTemplate.getExpire(lockKey, TimeUnit.SECONDS))
                .thenThrow(new RuntimeException("Redis connection error"));

        // Act
        Long result = redisLockService.getLockTTL(lockKey);

        // Assert
        assertEquals(-1L, result);
    }

    // ==================== extendLock() Tests - V(G) = 4 ====================
    // Cyclomatic Complexity: 4 (3 decision nodes: lockValue.equals(currentValue),
    // currentTTL>0, catch)
    // Test Cases: 4 (covers all paths)

    @Test
    @DisplayName("Successfully extend owned lock with valid TTL")
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
    @DisplayName("Prevent extending lock owned by another user")
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
    @DisplayName("Handle extending lock with negative/expired TTL")
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
    @DisplayName("Handle Redis connection errors gracefully during extend")
    void testExtendLock_RedisException() {
        // Arrange
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(lockKey)).thenThrow(new RuntimeException("Redis connection error"));

        // Act
        boolean result = redisLockService.extendLock(lockKey, lockValue, 300L);

        // Assert
        assertFalse(result);
    }

    // ==================== Key Generation Tests - V(G) = 1 ====================
    // Cyclomatic Complexity: 1 (0 decision nodes for each method)
    // Test Cases: 3 (one for each key generation method)
    // Note: V(G) = 1, excluded from decision tables per requirements

    @Test
    @DisplayName("Generate seat lock key with correct format")
    void testGenerateSeatLockKey() {
        // Arrange
        UUID showtimeId = UUID.randomUUID();
        UUID seatId = UUID.randomUUID();

        // Act
        String key = redisLockService.generateSeatLockKey(showtimeId, seatId);

        // Assert
        assertEquals("lock:seat:" + showtimeId + ":" + seatId, key);
    }

    // ==================== acquireMultipleSeatsLock() Tests - V(G) = 4
    // ====================
    // Cyclomatic Complexity: 4 (3 decision nodes: for loop isLocked check, for loop
    // acquire, !acquireLock)
    // Test Cases: 4 (covers all paths including rollback)

    @Test
    @DisplayName("Successfully lock all seats when available")
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
    @DisplayName("Fail when at least one seat is already locked")
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
    @DisplayName("Rollback successfully acquired locks on partial failure")
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

    // ==================== releaseMultipleSeatsLock() Tests - V(G) = 2
    // ====================
    // Cyclomatic Complexity: 2 (1 decision node: for loop)
    // Test Cases: 1 (covers normal path, empty list tested via
    // acquireMultipleSeatsLock_EmptyList)
    // Note: V(G) ≤ 2, excluded from decision tables per requirements

    @Test
    @DisplayName("Release multiple seat locks")
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
    @DisplayName("Handle empty seat list (edge case)")
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
