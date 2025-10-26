package com.api.moviebooking.services;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Scheduled task to cleanup expired seat locks
 * Runs every minute to release seats from expired locks
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class SeatLockCleanupScheduler {

    private final BookingService bookingService;

    /**
     * Runs every minute to cleanup expired locks
     */
    @Scheduled(fixedRate = 60000) // Every 60 seconds
    public void cleanupExpiredLocks() {
        log.debug("Running expired lock cleanup task");
        try {
            bookingService.cleanupExpiredLocks();
        } catch (Exception e) {
            log.error("Error during lock cleanup", e);
        }
    }
}
