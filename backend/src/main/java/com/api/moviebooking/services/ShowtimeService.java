package com.api.moviebooking.services;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.api.moviebooking.helpers.exceptions.ResourceNotFoundException;
import com.api.moviebooking.helpers.exceptions.EntityDeletionForbiddenException;
import com.api.moviebooking.helpers.mapstructs.ShowtimeMapper;
import com.api.moviebooking.models.dtos.showtime.AddShowtimeRequest;
import com.api.moviebooking.models.dtos.showtime.ShowtimeDataResponse;
import com.api.moviebooking.models.dtos.showtime.UpdateShowtimeRequest;
import com.api.moviebooking.models.entities.Movie;
import com.api.moviebooking.models.entities.Room;
import com.api.moviebooking.models.entities.Showtime;
import com.api.moviebooking.repositories.MovieRepo;
import com.api.moviebooking.repositories.RoomRepo;
import com.api.moviebooking.repositories.ShowtimeRepo;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ShowtimeService {

    private final ShowtimeRepo showtimeRepo;
    private final ShowtimeMapper showtimeMapper;
    private final RoomRepo roomRepo;
    private final MovieRepo movieRepo;
    private final ShowtimeSeatService showtimeSeatService;

    /**
     * findShowtimeById:
     * Predicate nodes (d): 0 -> V(G)=d+1=1
     * Nodes: none (just findById with exception)
     * Minimum test cases: 1
     */
    public Showtime findShowtimeById(UUID showtimeId) {
        return showtimeRepo.findById(showtimeId)
                .orElseThrow(() -> new ResourceNotFoundException("Showtime", "id", showtimeId));
    }

    /**
     * findRoomById:
     * Predicate nodes (d): 0 -> V(G)=d+1=1
     * Nodes: none (just findById with exception)
     * Minimum test cases: 1
     */
    private Room findRoomById(UUID roomId) {
        return roomRepo.findById(roomId)
                .orElseThrow(() -> new ResourceNotFoundException("Room", "id", roomId));
    }

    /**
     * findMovieById:
     * Predicate nodes (d): 0 -> V(G)=d+1=1
     * Nodes: none (just findById with exception)
     * Minimum test cases: 1
     */
    private Movie findMovieById(UUID movieId) {
        return movieRepo.findById(movieId)
                .orElseThrow(() -> new ResourceNotFoundException("Movie", "id", movieId));
    }

    /**
     * validateNoOverlap:
     * Predicate nodes (d): 2 -> V(G)=d+1=3
     * Nodes:
     * - showtimeId != null (ternary operator)
     * - existsOverlappingShowtime
     * Minimum test cases: 3
     */
    // Validate no overlap in the same room
    private void validateNoOverlap(UUID showtimeId, UUID roomId, LocalDateTime startTime, int movieDuration) {
        // Calculate end time based on movie duration
        LocalDateTime endTime = startTime.plusMinutes(movieDuration);

        // Use a placeholder UUID for new showtimes (won't match any existing)
        UUID checkId = (showtimeId != null) ? showtimeId : UUID.randomUUID();

        if (showtimeRepo.existsOverlappingShowtime(roomId, checkId, startTime, endTime)) {
            throw new IllegalArgumentException("This showtime overlaps with another showtime in the same room");
        }
    }

    /**
     * Add a new showtime (API: POST /showtimes)
     * Predicate nodes (d): 0 -> V(G)=d+1=1
     * Nodes: none (just validation through helper methods)
     * Minimum test cases: 1
     */
    @Transactional
    public ShowtimeDataResponse addShowtime(AddShowtimeRequest request) {
        Room room = findRoomById(request.getRoomId());
        Movie movie = findMovieById(request.getMovieId());

        // Validate no overlapping showtimes
        validateNoOverlap(null, request.getRoomId(), request.getStartTime(), movie.getDuration());

        Showtime newShowtime = showtimeMapper.toEntity(request);
        newShowtime.setRoom(room);
        newShowtime.setMovie(movie);

        showtimeRepo.save(newShowtime);
        showtimeSeatService.generateShowtimeSeats(newShowtime.getId());
        return showtimeMapper.toDataResponse(newShowtime);
    }

    /**
     * Update showtime details (API: PUT /showtimes/{showtimeId})
     * Predicate nodes (d): 9 -> V(G)=d+1=10
     * Nodes:
     * - request.getRoomId() != null (ternary)
     * - request.getMovieId() != null (ternary)
     * - request.getStartTime() != null (ternary)
     * - request.getMovieId() != null (for movie validation)
     * - !newRoomId.equals || !newMovieId.equals || !newStartTime.equals
     * - request.getRoomId() != null
     * - request.getMovieId() != null
     * - request.getFormat() != null
     * - request.getStartTime() != null
     */
    @Transactional
    public ShowtimeDataResponse updateShowtime(UUID showtimeId, UpdateShowtimeRequest request) {
        Showtime showtime = findShowtimeById(showtimeId);

        UUID newRoomId = (request.getRoomId() != null) ? request.getRoomId() : showtime.getRoom().getId();
        UUID newMovieId = (request.getMovieId() != null) ? request.getMovieId() : showtime.getMovie().getId();
        LocalDateTime newStartTime = (request.getStartTime() != null) ? request.getStartTime()
                : showtime.getStartTime();

        // Get movie for validation
        Movie movie = (request.getMovieId() != null) ? findMovieById(newMovieId) : showtime.getMovie();

        // Validate if room, movie, or start time changed
        if (!newRoomId.equals(showtime.getRoom().getId()) ||
                !newMovieId.equals(showtime.getMovie().getId()) ||
                !newStartTime.equals(showtime.getStartTime())) {
            validateNoOverlap(showtimeId, newRoomId, newStartTime, movie.getDuration());
        }

        if (request.getRoomId() != null) {
            Room room = findRoomById(request.getRoomId());
            showtime.setRoom(room);
        }
        if (request.getMovieId() != null) {
            showtime.setMovie(movie);
        }
        if (request.getFormat() != null) {
            showtime.setFormat(request.getFormat());
        }
        if (request.getStartTime() != null) {
            showtime.setStartTime(request.getStartTime());
        }

        showtimeRepo.save(showtime);
        return showtimeMapper.toDataResponse(showtime);
    }

    /**
     * Delete a showtime (API: DELETE /showtimes/{showtimeId})
     * Predicate nodes (d): 4 -> V(G) = d + 1 = 5
     * Nodes: findShowtimeById, !isEmpty(seatLocks), !isEmpty(showtimeSeats),
     * !isEmpty(bookings)
     */
    @Transactional
    public void deleteShowtime(UUID showtimeId) {
        Showtime showtime = findShowtimeById(showtimeId);

        if (!showtime.getSeatLocks().isEmpty()) {
            throw new EntityDeletionForbiddenException();
        }

        if (!showtime.getShowtimeSeats().isEmpty()) {
            throw new EntityDeletionForbiddenException();
        }

        if (!showtime.getBookings().isEmpty()) {
            throw new EntityDeletionForbiddenException();
        }

        showtimeRepo.delete(showtime);
    }

    /**
     * Get showtime details by ID (API: GET /showtimes/{showtimeId})
     * Predicate nodes (d): 1 -> V(G) = d + 1 = 2
     * Nodes: findShowtimeById
     */
    public ShowtimeDataResponse getShowtime(UUID showtimeId) {
        Showtime showtime = findShowtimeById(showtimeId);
        return showtimeMapper.toDataResponse(showtime);
    }

    /**
     * Get all showtimes (API: GET /showtimes)
     * Predicate nodes (d): 0 -> V(G) = d + 1 = 1
     * Nodes: none
     */
    public List<ShowtimeDataResponse> getAllShowtimes() {
        return showtimeRepo.findAll().stream()
                .map(showtimeMapper::toDataResponse)
                .collect(Collectors.toList());
    }

    /**
     * Get all showtimes for a specific movie (API: GET /showtimes/movie/{movieId})
     * Predicate nodes (d): 1 -> V(G) = d + 1 = 2
     * Nodes: findMovieById
     */
    public List<ShowtimeDataResponse> getShowtimesByMovie(UUID movieId) {
        // Verify movie exists
        findMovieById(movieId);

        return showtimeRepo.findByMovieId(movieId).stream()
                .map(showtimeMapper::toDataResponse)
                .collect(Collectors.toList());
    }

    /**
     * Get upcoming showtimes for a specific movie (API: GET
     * /showtimes/movie/{movieId}/upcoming)
     * Predicate nodes (d): 1 -> V(G) = d + 1 = 2
     * Nodes: findMovieById
     */
    public List<ShowtimeDataResponse> getUpcomingShowtimesByMovie(UUID movieId) {
        // Verify movie exists
        findMovieById(movieId);

        return showtimeRepo.findUpcomingShowtimesByMovie(movieId, LocalDateTime.now()).stream()
                .map(showtimeMapper::toDataResponse)
                .collect(Collectors.toList());
    }

    /**
     * Get all showtimes for a specific room (API: GET /showtimes/room/{roomId})
     * Predicate nodes (d): 1 -> V(G) = d + 1 = 2
     * Nodes: findRoomById
     */
    public List<ShowtimeDataResponse> getShowtimesByRoom(UUID roomId) {
        // Verify room exists
        findRoomById(roomId);

        return showtimeRepo.findByRoomId(roomId).stream()
                .map(showtimeMapper::toDataResponse)
                .collect(Collectors.toList());
    }

    /**
     * Get showtimes for a movie within date range (API: GET
     * /showtimes/movie/{movieId}/date-range)
     * Predicate nodes (d): 1 -> V(G) = d + 1 = 2
     * Nodes: findMovieById
     */
    public List<ShowtimeDataResponse> getShowtimesByMovieAndDateRange(UUID movieId, LocalDateTime startDate,
            LocalDateTime endDate) {
        // Verify movie exists
        findMovieById(movieId);

        return showtimeRepo.findByMovieAndDateRange(movieId, startDate, endDate).stream()
                .map(showtimeMapper::toDataResponse)
                .collect(Collectors.toList());
    }
}
