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
public class CleanupScheduler {

    private final BookingService bookingService;
    private final CheckoutService checkoutService;

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

    /**
     * Runs every 60 seconds to expire pending payments that exceeded timeout
     * Releases seats back to inventory for bookings with PENDING_PAYMENT status
     * whose paymentExpiresAt timestamp has passed
     */
    @Scheduled(fixedDelay = 60000, initialDelay = 60000)
    public void cleanupExpiredPendingPayments() {
        log.debug("Running payment timeout cleanup task");
        try {
            checkoutService.cleanupExpiredPendingPayments();
        } catch (Exception e) {
            log.error("Error during payment timeout cleanup", e);
        }
    }

}
