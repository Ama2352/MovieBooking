package com.api.moviebooking.repositories;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.api.moviebooking.models.entities.SeatLock;

public interface SeatLockRepo extends JpaRepository<SeatLock, UUID> {

        /**
         * Find active lock by lock owner ID and showtime
         * Works for both authenticated users and guest sessions
         */
        @Query("SELECT sl FROM SeatLock sl WHERE sl.lockOwnerId = :lockOwnerId " +
                        "AND sl.showtime.id = :showtimeId AND sl.active = true")
        Optional<SeatLock> findActiveLockByOwnerAndShowtime(
                        @Param("lockOwnerId") String lockOwnerId,
                        @Param("showtimeId") UUID showtimeId);

        /**
         * Find all active locks for a lock owner (across all showtimes)
         * Works for both authenticated users and guest sessions
         */
        @Query("SELECT sl FROM SeatLock sl WHERE sl.lockOwnerId = :lockOwnerId AND sl.active = true")
        List<SeatLock> findAllActiveLocksForOwner(@Param("lockOwnerId") String lockOwnerId);

        /**
         * Find all expired locks
         */
        @Query("SELECT sl FROM SeatLock sl WHERE sl.expiresAt < :now AND sl.active = true")
        List<SeatLock> findExpiredLocks(@Param("now") LocalDateTime now);
}