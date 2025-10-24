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
    @Query("SELECT COUNT(s) > 0 FROM Showtime s WHERE s.room.id = :roomId AND " + //Counting showtimes in the same room(if any -> exist)
           "s.id <> :showtimeId AND " + //Excluding the current showtime (for updates)
           "s.startTime < :endTime AND " + 
           "FUNCTION('TIMESTAMPADD', HOUR, CAST(s.movie.duration AS int) / 60, s.startTime) > :startTime") //Calculating db showtime end time based on movie duration
    boolean existsOverlappingShowtime(@Param("roomId") UUID roomId, 
                                     @Param("showtimeId") UUID showtimeId,
                                     @Param("startTime") LocalDateTime startTime, 
                                     @Param("endTime") LocalDateTime endTime);

    // Find upcoming showtimes for a movie
    @Query("SELECT s FROM Showtime s WHERE s.movie.id = :movieId AND s.startTime >= :now ORDER BY s.startTime")
    List<Showtime> findUpcomingShowtimesByMovie(@Param("movieId") UUID movieId, @Param("now") LocalDateTime now);
}
