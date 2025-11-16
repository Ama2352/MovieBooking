package com.api.moviebooking.repositories;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.api.moviebooking.models.entities.Showtime;

public interface ShowtimeRepo extends JpaRepository<Showtime, UUID> {

    // Find showtimes by movie
    List<Showtime> findByMovieId(UUID movieId);

    // Find showtimes by room
    List<Showtime> findByRoomId(UUID roomId);

    // Find showtimes by movie and start time range
    @Query("SELECT s FROM Showtime s WHERE s.movie.id = :movieId AND s.startTime BETWEEN :startDate AND :endDate")
    List<Showtime> findByMovieAndDateRange(@Param("movieId") UUID movieId,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate);

    // Check for overlapping showtimes in the same room
    // Used for validation to ensure no time conflicts
    @Query("SELECT COUNT(s) > 0 FROM Showtime s WHERE " +
            "s.room.id = :roomId AND " + // Only check showtimes in the same room
            "s.id <> :showtimeId AND " + // Exclude the current showtime (important for updates)
            "s.startTime < :endTime AND " + // Existing showtime starts before new showtime ends

            // Existing showtime end = startTime + movie duration (in MINUTES)
            // Using TIMESTAMPADD(MINUTE, ...) ensures correct end time (no integer division
            // issues)
            "FUNCTION('TIMESTAMPADD', MINUTE, s.movie.duration, s.startTime) > :startTime") // Existing end > new start
    boolean existsOverlappingShowtime(@Param("roomId") UUID roomId,
            @Param("showtimeId") UUID showtimeId,
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime);

    // Find upcoming showtimes for a movie
    @Query("SELECT s FROM Showtime s WHERE s.movie.id = :movieId AND s.startTime >= :now ORDER BY s.startTime")
    List<Showtime> findUpcomingShowtimesByMovie(@Param("movieId") UUID movieId, @Param("now") LocalDateTime now);
}
