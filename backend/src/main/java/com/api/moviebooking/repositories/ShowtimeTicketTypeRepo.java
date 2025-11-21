package com.api.moviebooking.repositories;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.api.moviebooking.models.entities.ShowtimeTicketType;

public interface ShowtimeTicketTypeRepo extends JpaRepository<ShowtimeTicketType, UUID> {

    /**
     * Find all active ticket types for a specific showtime
     */
    @Query("SELECT stt FROM ShowtimeTicketType stt " +
           "WHERE stt.showtime.id = :showtimeId AND stt.isActive = true " +
           "ORDER BY stt.ticketType.sortOrder ASC")
    List<ShowtimeTicketType> findActiveTicketTypesByShowtime(@Param("showtimeId") UUID showtimeId);

    /**
     * Find all ticket type assignments for a showtime (both active and inactive)
     */
    @Query("SELECT stt FROM ShowtimeTicketType stt " +
           "WHERE stt.showtime.id = :showtimeId " +
           "ORDER BY stt.ticketType.sortOrder ASC")
    List<ShowtimeTicketType> findAllByShowtime(@Param("showtimeId") UUID showtimeId);

    /**
     * Check if a specific ticket type is active for a showtime
     */
    @Query("SELECT CASE WHEN COUNT(stt) > 0 THEN true ELSE false END " +
           "FROM ShowtimeTicketType stt " +
           "WHERE stt.showtime.id = :showtimeId AND stt.ticketType.id = :ticketTypeId AND stt.isActive = true")
    boolean existsActiveByShowtimeAndTicketType(@Param("showtimeId") UUID showtimeId, @Param("ticketTypeId") UUID ticketTypeId);

    /**
     * Find a specific showtime-ticket type assignment
     */
    @Query("SELECT stt FROM ShowtimeTicketType stt " +
           "WHERE stt.showtime.id = :showtimeId AND stt.ticketType.id = :ticketTypeId")
    List<ShowtimeTicketType> findByShowtimeAndTicketType(@Param("showtimeId") UUID showtimeId, @Param("ticketTypeId") UUID ticketTypeId);
}
