package com.api.moviebooking.repositories;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.api.moviebooking.models.entities.Booking;
import com.api.moviebooking.models.enums.BookingStatus;

public interface BookingRepo extends JpaRepository<Booking, UUID> {

        /**
         * Find bookings by user ID
         */
        @Query("SELECT b FROM Booking b WHERE b.user.id = :userId ORDER BY b.bookedAt DESC")
        List<Booking> findByUserId(@Param("userId") UUID userId);

        /**
         * Find bookings by showtime ID
         */
        @Query("SELECT b FROM Booking b WHERE b.showtime.id = :showtimeId")
        List<Booking> findByShowtimeId(@Param("showtimeId") UUID showtimeId);

        /**
         * Find booking by ID and user ID (for authorization)
         */
        @Query("SELECT b FROM Booking b WHERE b.id = :bookingId AND b.user.id = :userId")
        Optional<Booking> findByIdAndUserId(@Param("bookingId") UUID bookingId, @Param("userId") UUID userId);

        /**
         * Find bookings by status
         */
        @Query("SELECT b FROM Booking b WHERE b.status = :status")
        List<Booking> findByStatus(@Param("status") BookingStatus status);

        /**
         * Find user bookings within date range
         */
        @Query("SELECT b FROM Booking b WHERE b.user.id = :userId " +
                        "AND b.bookedAt BETWEEN :startDate AND :endDate ORDER BY b.bookedAt DESC")
        List<Booking> findByUserIdAndDateRange(
                        @Param("userId") UUID userId,
                        @Param("startDate") LocalDateTime startDate,
                        @Param("endDate") LocalDateTime endDate);

        /**
         * Count user's active bookings
         */
        @Query("SELECT COUNT(b) FROM Booking b WHERE b.user.id = :userId " +
                        "AND (b.status = com.api.moviebooking.models.enums.BookingStatus.CONFIRMED " +
                        "OR b.status = com.api.moviebooking.models.enums.BookingStatus.PENDING_PAYMENT)")
        long countActiveBookingsByUserId(@Param("userId") UUID userId);

        /**
         * Count total bookings by user ID (for dependency check)
         */
        @Query("SELECT COUNT(b) FROM Booking b WHERE b.user.id = :userId")
        long countByUserId(@Param("userId") UUID userId);

        List<Booking> findByStatusAndPaymentExpiresAtBefore(BookingStatus status, LocalDateTime timestamp);
}
