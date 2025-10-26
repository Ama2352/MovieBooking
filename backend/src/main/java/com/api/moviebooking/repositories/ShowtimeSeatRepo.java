package com.api.moviebooking.repositories;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.api.moviebooking.models.entities.ShowtimeSeat;
import com.api.moviebooking.models.enums.SeatStatus;

public interface ShowtimeSeatRepo extends JpaRepository<ShowtimeSeat, UUID> {

    /**
     * Find showtime seats by showtime ID
     */
    @Query("SELECT ss FROM ShowtimeSeat ss WHERE ss.showtime.id = :showtimeId")
    List<ShowtimeSeat> findByShowtimeId(@Param("showtimeId") UUID showtimeId);

    /**
     * Find available seats for a showtime
     */
    @Query("SELECT ss FROM ShowtimeSeat ss WHERE ss.showtime.id = :showtimeId AND ss.status = :status")
    List<ShowtimeSeat> findByShowtimeIdAndStatus(
            @Param("showtimeId") UUID showtimeId,
            @Param("status") SeatStatus status);

    /**
     * Find seats by IDs and showtime
     */
    @Query("SELECT ss FROM ShowtimeSeat ss WHERE ss.id IN :seatIds AND ss.showtime.id = :showtimeId")
    List<ShowtimeSeat> findByIdsAndShowtime(
            @Param("seatIds") List<UUID> seatIds,
            @Param("showtimeId") UUID showtimeId);

    /**
     * Update seat status
     */
    @Modifying
    @Query("UPDATE ShowtimeSeat ss SET ss.status = :status WHERE ss.id = :seatId")
    void updateSeatStatus(@Param("seatId") UUID seatId, @Param("status") SeatStatus status);

    /**
     * Update multiple seats status
     */
    @Modifying
    @Query("UPDATE ShowtimeSeat ss SET ss.status = :status WHERE ss.id IN :seatIds")
    void updateMultipleSeatsStatus(@Param("seatIds") List<UUID> seatIds, @Param("status") SeatStatus status);

    /**
     * Check if all seats are available
     */
    @Query("SELECT COUNT(ss) = :count FROM ShowtimeSeat ss WHERE ss.id IN :seatIds " +
            "AND ss.showtime.id = :showtimeId AND ss.status = com.api.moviebooking.models.enums.SeatStatus.AVAILABLE")
    boolean areAllSeatsAvailable(
            @Param("seatIds") List<UUID> seatIds,
            @Param("showtimeId") UUID showtimeId,
            @Param("count") long count);

    /**
     * Find locked or booked seats from a list
     */
    @Query("SELECT ss FROM ShowtimeSeat ss WHERE ss.id IN :seatIds " +
            "AND (ss.status = com.api.moviebooking.models.enums.SeatStatus.LOCKED OR ss.status = com.api.moviebooking.models.enums.SeatStatus.BOOKED)")
    List<ShowtimeSeat> findUnavailableSeats(@Param("seatIds") List<UUID> seatIds);
}
