package com.api.moviebooking.repositories;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.api.moviebooking.models.entities.SeatLock;

public interface SeatLockRepo extends JpaRepository<SeatLock, UUID> {

        /**
         * Find active lock by lock key (Redis key)
         */
        Optional<SeatLock> findByLockKeyAndActiveTrue(String lockKey);

        /**
         * Find active lock for a user and showtime
         */
        @Query("SELECT sl FROM SeatLock sl WHERE sl.user.id = :userId " +
                        "AND sl.showtime.id = :showtimeId AND sl.active = true")
        Optional<SeatLock> findActiveLockByUserAndShowtime(
                        @Param("userId") UUID userId,
                        @Param("showtimeId") UUID showtimeId);

        /**
         * Find all expired locks
         */
        @Query("SELECT sl FROM SeatLock sl WHERE sl.expiresAt < :now AND sl.active = true")
        List<SeatLock> findExpiredLocks(@Param("now") LocalDateTime now);

        /**
         * Find all active locks for a showtime
         */
        @Query("SELECT sl FROM SeatLock sl WHERE sl.showtime.id = :showtimeId AND sl.active = true")
        List<SeatLock> findActiveLocksForShowtime(@Param("showtimeId") UUID showtimeId);

        /**
         * Check if user has an active lock for any showtime
         */
        @Query("SELECT COUNT(sl) > 0 FROM SeatLock sl WHERE sl.user.id = :userId AND sl.active = true")
        boolean hasActiveLock(@Param("userId") UUID userId);

        /**
         * Find all active locks for a user (across all showtimes)
         */
        @Query("SELECT sl FROM SeatLock sl WHERE sl.user.id = :userId AND sl.active = true")
        List<SeatLock> findAllActiveLocksForUser(@Param("userId") UUID userId);

        /**
         * Deactivate a lock
         */
        @Modifying
        @Query("UPDATE SeatLock sl SET sl.active = false WHERE sl.id = :lockId")
        void deactivateLock(@Param("lockId") UUID lockId);

        /**
         * Deactivate all expired locks
         */
        @Modifying
        @Query("UPDATE SeatLock sl SET sl.active = false WHERE sl.expiresAt < :now AND sl.active = true")
        int deactivateExpiredLocks(@Param("now") LocalDateTime now);
}